package spring.abtechzone.modules.shipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.shipment.client.GhnLocationClient;
import spring.abtechzone.modules.shipment.config.GhnShippingProperties;
import spring.abtechzone.modules.shipment.dto.GhnDistrictData;
import spring.abtechzone.modules.shipment.dto.GhnProvinceData;
import spring.abtechzone.modules.shipment.dto.GhnWardData;
import spring.abtechzone.modules.shipment.service.ShippingLocationService;

@ExtendWith(MockitoExtension.class)
class ShippingLocationServiceTest {

    @Mock
    GhnLocationClient client;

    @Mock
    GhnShippingProperties properties;

    @InjectMocks
    ShippingLocationService service;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getLocationCacheTtlSeconds()).thenReturn(60L);
    }

    @Test
    void resolveRoute_returnsCanonicalDeliveryRouteAndCachesLookups() {
        when(client.getProvinces()).thenReturn(List.of(new GhnProvinceData(202, "Hồ Chí Minh")));
        when(client.getDistricts(202)).thenReturn(List.of(new GhnDistrictData(1442, "Quận 1", 2)));
        when(client.getWards(1442)).thenReturn(List.of(new GhnWardData("20308", "Phường Bến Nghé", 3)));

        var route = service.resolveRoute(202, 1442, " 20308 ");
        assertThat(route.province()).isEqualTo("Hồ Chí Minh");
        assertThat(route.district()).isEqualTo("Quận 1");
        assertThat(route.ward()).isEqualTo("Phường Bến Nghé");

        service.resolveRoute(202, 1442, "20308");
        verify(client, times(1)).getProvinces();
        verify(client, times(1)).getDistricts(202);
        verify(client, times(1)).getWards(1442);
    }

    @Test
    void resolveRoute_rejectsUnsupportedWard() {
        when(client.getProvinces()).thenReturn(List.of(new GhnProvinceData(202, "Hồ Chí Minh")));
        when(client.getDistricts(202)).thenReturn(List.of(new GhnDistrictData(1442, "Quận 1", 2)));
        when(client.getWards(1442)).thenReturn(List.of(new GhnWardData("20308", "Phường Bến Nghé", 1)));

        assertThatThrownBy(() -> service.resolveRoute(202, 1442, "20308"))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.SHIPPING_ADDRESS_INVALID.getMessage());
    }
}
