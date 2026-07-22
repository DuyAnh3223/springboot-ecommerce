package spring.abtechzone.modules.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import spring.abtechzone.modules.user.dto.request.AddressRequest;
import spring.abtechzone.modules.user.dto.response.AddressResponse;
import spring.abtechzone.modules.user.entity.Address;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    Address toAddress(AddressRequest request);

    AddressResponse toAddressResponse(Address address);

    @Mapping(target = "user", ignore = true)
    void updateAddress(@MappingTarget Address address, AddressRequest request);
}
