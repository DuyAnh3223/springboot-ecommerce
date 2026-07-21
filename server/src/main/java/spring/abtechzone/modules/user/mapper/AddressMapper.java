package spring.abtechzone.modules.user.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import spring.abtechzone.modules.user.dto.request.AddressRequest;
import spring.abtechzone.modules.user.dto.response.AddressResponse;
import spring.abtechzone.modules.user.entity.UserAddress;

@Mapper(componentModel = "spring")
public interface AddressMapper {


    UserAddress toAddress(AddressRequest request);

    AddressResponse toAddressResponse(UserAddress address);

    @Mapping(target = "user", ignore = true)
    void updateAddress(@MappingTarget UserAddress address, AddressRequest request);
}
