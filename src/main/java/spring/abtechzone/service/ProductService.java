package spring.abtechzone.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.dto.request.ProductRequest;
import spring.abtechzone.dto.response.ProductResponse;
import spring.abtechzone.entity.Product;
import spring.abtechzone.exception.AppException;
import spring.abtechzone.exception.ErrorCode;
import spring.abtechzone.mapper.ProductMapper;
import spring.abtechzone.mapper.ProductSkuMapper;
import spring.abtechzone.repository.ProductRepository;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductService {
    ProductRepository productRepository;
    ProductMapper productMapper;
    ProductSkuMapper productSkuMapper;

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = productMapper.toProduct(request);

        try {
            product = productRepository.save(product);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.PRODUCT_SKU_EXISTS);
        }

        return productMapper.toProductResponse(product);
    }

    private Product findProductById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    public ProductResponse getProduct(Long id) {
        return productMapper.toProductResponse(findProductById(id));
    }

    public List<ProductResponse> getProducts() {
        return productRepository.findAll().stream()
                .map(productMapper::toProductResponse)
                .toList();
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        // 1. Tìm sản phẩm hiện tại trong Database
        Product product = findProductById(id);

        // 2. Ghi đè các trường thông tin cơ bản từ DTO sang Entity hiện tại
        productMapper.updateProduct(product, request);

        // 3. Lưu sản phẩm
        try {
            product = productRepository.save(product);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.PRODUCT_SKU_EXISTS);
        }

        // 4. Trả về kết quả Response DTO
        return productMapper.toProductResponse(product);
    }

    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
    }
}
