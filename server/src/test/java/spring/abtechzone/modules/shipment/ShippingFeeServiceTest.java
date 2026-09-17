package spring.abtechzone.modules.shipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.shipment.client.GhnFeeClient;
import spring.abtechzone.modules.shipment.config.GhnShippingProperties;
import spring.abtechzone.modules.shipment.dto.GhnFeeRequest;
import spring.abtechzone.modules.shipment.dto.ShippingAddressData;
import spring.abtechzone.modules.shipment.service.ShippingFeeService;

@ExtendWith(MockitoExtension.class)
class ShippingFeeServiceTest {

    @Mock
    GhnFeeClient ghnFeeClient;

    @Mock
    GhnShippingProperties properties;

    @InjectMocks
    ShippingFeeService shippingFeeService;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getFromDistrictId()).thenReturn(1450);
        lenient().when(properties.getFromWardCode()).thenReturn("21008");
        lenient().when(properties.getFixedWeightGram()).thenReturn(500);
        lenient().when(properties.getServiceTypeId()).thenReturn(2);
    }

    @Test
    void calculate_usesConfiguredFixedWeightAndDestinationRoute() {
        when(ghnFeeClient.calculateFee(any())).thenReturn(42000);

        var result = shippingFeeService.calculate(new ShippingAddressData(
                null,
                "Nguyen Van A",
                "0900000000",
                "HCM",
                "District 1",
                "Ward 1",
                "1 Main Street",
                202,
                1442,
                "20308"));

        assertThat(result.totalFee()).isEqualByComparingTo(BigDecimal.valueOf(42000));
        assertThat(result.quotedAt()).isNotNull();
        ArgumentCaptor<GhnFeeRequest> requestCaptor = ArgumentCaptor.forClass(GhnFeeRequest.class);
        verify(ghnFeeClient).calculateFee(requestCaptor.capture());
        assertThat(requestCaptor.getValue().fromDistrictId()).isEqualTo(1450);
        assertThat(requestCaptor.getValue().fromWardCode()).isEqualTo("21008");
        assertThat(requestCaptor.getValue().toDistrictId()).isEqualTo(1442);
        assertThat(requestCaptor.getValue().toWardCode()).isEqualTo("20308");
        assertThat(requestCaptor.getValue().weight()).isEqualTo(500);
    }

    @Test
    void calculate_rejectsMissingDestinationRouteBeforeCallingProvider() {
        ShippingAddressData missingRoute = new ShippingAddressData(
                null, "Nguyen Van A", "0900000000", "HCM", "District 1", "Ward 1", "1 Main Street", 202, null, null);

        assertThatThrownBy(() -> shippingFeeService.calculate(missingRoute))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.SHIPPING_ADDRESS_INVALID.getMessage());
        verify(ghnFeeClient, never()).calculateFee(any());
    }
}
