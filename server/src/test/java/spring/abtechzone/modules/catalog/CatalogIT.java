package spring.abtechzone.modules.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import spring.abtechzone.common.BaseIT;
import spring.abtechzone.modules.category.entity.Attribute;
import spring.abtechzone.modules.category.entity.Brand;
import spring.abtechzone.modules.category.entity.Category;
import spring.abtechzone.modules.category.entity.CategoryAttribute;
import spring.abtechzone.modules.category.repository.AttributeRepository;
import spring.abtechzone.modules.category.repository.BrandRepository;
import spring.abtechzone.modules.category.repository.CategoryAttributeRepository;
import spring.abtechzone.modules.category.repository.CategoryRepository;
import spring.abtechzone.modules.product.entity.Product;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.repository.ProductRepository;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;

@AutoConfigureMockMvc
class CatalogIT extends BaseIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AttributeRepository attributeRepository;

    @Autowired
    private CategoryAttributeRepository categoryAttributeRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductSkuRepository productSkuRepository;

    @Autowired
    private spring.abtechzone.modules.cart.repository.CartItemRepository cartItemRepository;

    @Autowired
    private spring.abtechzone.modules.cart.repository.CartRepository cartRepository;

    @Autowired
    private spring.abtechzone.modules.order.repository.OrderItemRepository orderItemRepository;

    @Autowired
    private spring.abtechzone.modules.order.repository.OrderRepository orderRepository;

    private Category category;
    private Brand brandA;
    private Brand brandB;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        productSkuRepository.deleteAll();
        productRepository.deleteAll();
        categoryAttributeRepository.deleteAll();
        attributeRepository.deleteAll();
        brandRepository.deleteAll();
        categoryRepository.deleteAll();

        category = new Category();
        category.setName("Laptop");
        category.setSlug("laptop");
        category.setIsActive(true);
        category.setSortOrder(1);
        category = categoryRepository.save(category);

        brandA = new Brand();
        brandA.setName("Brand A");
        brandA.setSlug("brand-a");
        brandA = brandRepository.save(brandA);

        brandB = new Brand();
        brandB.setName("Brand B");
        brandB.setSlug("brand-b");
        brandB = brandRepository.save(brandB);

        Attribute colorAttr = createAttribute("Color", "Color", "STRING");

        Attribute ramAttr = createAttribute("RAM", "RAM", "NUMBER");
        ramAttr.setUnit("GB");
        ramAttr = attributeRepository.save(ramAttr);

        createCategoryAttribute(category, colorAttr, true, true, false, false, 1);
        createCategoryAttribute(category, ramAttr, true, false, true, false, 2);

        Product p1 = createProduct("Laptop A1", brandA, Map.of("RAM", 8, "Color", "Silver"), 10_000_000, 12_000_000);
        createSku(p1, "SKU-A1-SILVER", 10_000_000, 5, Map.of("Color", "Silver"));

        Product p2 = createProduct("Laptop A2", brandA, Map.of("RAM", 16, "Color", "Black"), 15_000_000, 15_000_000);
        createSku(p2, "SKU-A2-BLACK", 15_000_000, 0, Map.of("Color", "Black"));

        Product p3 = createProduct("Laptop B1", brandB, Map.of("RAM", 32, "Color", "Green"), 25_000_000, 30_000_000);
        createSku(p3, "SKU-B1-GREEN", 25_000_000, 0, Map.of("Color", "Green"));
        createSku(p3, "SKU-B1-BLUE", 30_000_000, 10, Map.of("Color", "Blue"));
    }

    @Test
    void getCategoryFacets_brandAndPriceBounds() throws Exception {
        mockMvc.perform(get("/api/v1/public/catalog/category/laptop/facets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.categoryName").value("Laptop"))
                .andExpect(jsonPath("$.result.brands.length()").value(2))
                .andExpect(jsonPath("$.result.priceMin").value(10_000_000))
                .andExpect(jsonPath("$.result.priceMax").value(30_000_000));
    }

    @Test
    void searchProducts_withBrandFilter() throws Exception {
        mockMvc.perform(get("/api/v1/public/catalog/category/laptop/products")
                        .param("brandId", String.valueOf(brandA.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(2));
    }

    @Test
    void searchProducts_withProductJsonbNumberAttributeFilter() throws Exception {
        mockMvc.perform(get("/api/v1/public/catalog/category/laptop/products").param("attr_RAM", "min:16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(2));
    }

    @Test
    void searchProducts_sortByJsonbAttribute() throws Exception {
        mockMvc.perform(get("/api/v1/public/catalog/category/laptop/products")
                        .param("sortBy", "RAM")
                        .param("order", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].name").value("Laptop B1"));
    }

    private Attribute createAttribute(String name, String code, String dataType) {
        Attribute attr = new Attribute();
        attr.setName(name);
        attr.setCode(code);
        attr.setDataType(dataType);
        attr.setCreatedAt(OffsetDateTime.now());
        attr.setUpdatedAt(OffsetDateTime.now());
        return attributeRepository.save(attr);
    }

    private void createCategoryAttribute(
            Category cat,
            Attribute attr,
            boolean filterable,
            boolean variantDefining,
            boolean sortable,
            boolean multiValue,
            int sortOrder) {
        CategoryAttribute ca = new CategoryAttribute();
        ca.setCategory(cat);
        ca.setAttribute(attr);
        ca.setIsFilterable(filterable);
        ca.setIsVariantDefining(variantDefining);
        ca.setIsSortable(sortable);
        ca.setIsMultiValue(multiValue);
        ca.setIsRequired(false);
        ca.setIsCompatibilityKey(false);
        ca.setSortOrder(sortOrder);
        ca.setCreatedAt(OffsetDateTime.now());
        ca.setUpdatedAt(OffsetDateTime.now());
        categoryAttributeRepository.save(ca);
    }

    private Product createProduct(
            String name, Brand brand, Map<String, Object> attributes, long priceMinVal, long priceMaxVal) {
        Product p = Product.builder()
                .name(name)
                .slug(name.toLowerCase().replace(" ", "-"))
                .published(true)
                .category(category)
                .brand(brand)
                .attributes(attributes)
                .priceMin(BigDecimal.valueOf(priceMinVal))
                .priceMax(BigDecimal.valueOf(priceMaxVal))
                .totalStock(10)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        return productRepository.save(p);
    }

    private ProductSku createSku(
            Product product, String skuCode, long priceVal, int stock, Map<String, Object> attributes) {
        ProductSku sku = ProductSku.builder()
                .product(product)
                .sku(skuCode)
                .price(BigDecimal.valueOf(priceVal))
                .stock(stock)
                .attributes(attributes)
                .active(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        return productSkuRepository.save(sku);
    }
}
