package spring.abtechzone.modules.catalog.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.catalog.dto.request.BrandRequest;
import spring.abtechzone.modules.catalog.dto.response.BrandResponse;
import spring.abtechzone.modules.catalog.entity.Brand;
import spring.abtechzone.modules.catalog.mapper.BrandMapper;
import spring.abtechzone.modules.catalog.repository.BrandRepository;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BrandService {
    BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    public BrandResponse create(BrandRequest request) {
        Boolean existedBrand = brandRepository.existsByName(request.getName());
        if (Boolean.TRUE.equals(existedBrand)) {
            throw new AppException(ErrorCode.BRAND_EXISTED);
        }
        Brand brand = brandMapper.toBrand(request);
        brandRepository.save(brand);

        return brandMapper.toBrandResponse(brand);
    }

    public List<Brand> getBrands() {
        return brandRepository.findAll();
    }

    public BrandResponse getBrand(Long id) {
        return brandMapper.toBrandResponse(
                brandRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND)));
    }

    public BrandResponse update(Long id, BrandRequest request) {
        Brand brand = brandRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));
        brand.setName(request.getName());
        brand.setSlug(request.getSlug());
        brand.setLogoUrl(request.getLogoUrl());
        brandRepository.save(brand);
        return brandMapper.toBrandResponse(brand);
    }

    public void deleteBrand(Long id) {
        brandRepository.deleteById(id);
    }
}
