package spring.abtechzone.category;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import spring.abtechzone.common.service.AwsS3FileService;
import spring.abtechzone.common.service.S3ObjectLifecycleHelper;
import spring.abtechzone.modules.category.dto.request.CategoryRequest;
import spring.abtechzone.modules.category.dto.request.CategorySearchRequest;
import spring.abtechzone.modules.category.dto.response.CategoryResponse;
import spring.abtechzone.modules.category.entity.Category;
import spring.abtechzone.modules.category.mapper.CategoryMapper;
import spring.abtechzone.modules.category.repository.CategoryRepository;
import spring.abtechzone.modules.category.service.CategoryService;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private AwsS3FileService awsS3FileService;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        S3ObjectLifecycleHelper s3ObjectLifecycleHelper = new S3ObjectLifecycleHelper(awsS3FileService);
        categoryService =
                new CategoryService(categoryRepository, categoryMapper, awsS3FileService, s3ObjectLifecycleHelper);
    }

    @Test
    void create_Success_StoresRawS3KeyAndResolvesSignedUrl() {
        String fullUrl = "https://dj1x0wm4k4ps6.cloudfront.net/categories/laptop.png";
        String rawKey = "categories/laptop.png";
        String signedUrl = "https://dj1x0wm4k4ps6.cloudfront.net/categories/laptop.png?Expires=999&Signature=xyz";

        CategoryRequest request = new CategoryRequest("Laptop", "laptop", fullUrl);
        Category category = new Category();
        category.setId(1L);
        category.setName("Laptop");
        category.setSlug("laptop");
        category.setThumbnail(fullUrl);

        CategoryResponse mappedResponse = CategoryResponse.builder()
                .id(1L)
                .name("Laptop")
                .slug("laptop")
                .thumbnail(signedUrl)
                .build();

        when(categoryRepository.existsByName("Laptop")).thenReturn(false);
        when(categoryMapper.toCategory(request)).thenReturn(category);
        when(awsS3FileService.extractS3Key(fullUrl)).thenReturn(rawKey);
        when(categoryMapper.toCategoryResponse(category, awsS3FileService)).thenReturn(mappedResponse);

        CategoryResponse result = categoryService.create(request);

        assertNotNull(result);
        assertEquals(signedUrl, result.getThumbnail());
        assertEquals(rawKey, category.getThumbnail());
        verify(categoryRepository, times(1)).save(category);
    }

    @Test
    void updateCategory_SavesRawKeyAndDeletesOldS3Key() {
        String oldKey = "categories/old-123.png";
        String newUrl = "https://dj1x0wm4k4ps6.cloudfront.net/categories/new-456.png";
        String newKey = "categories/new-456.png";
        String signedUrl = "https://dj1x0wm4k4ps6.cloudfront.net/categories/new-456.png?Expires=999&Signature=xyz";

        Category category = new Category();
        category.setId(1L);
        category.setThumbnail(oldKey);

        CategoryRequest request = new CategoryRequest("Laptop", "laptop", newUrl);
        CategoryResponse mappedResponse =
                CategoryResponse.builder().id(1L).thumbnail(signedUrl).build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(awsS3FileService.extractS3Key(newUrl)).thenReturn(newKey);
        when(awsS3FileService.extractS3Key(oldKey)).thenReturn(oldKey);
        when(categoryMapper.toCategoryResponse(category, awsS3FileService)).thenReturn(mappedResponse);

        CategoryResponse response = categoryService.updateCategory(1L, request);

        verify(awsS3FileService, times(1)).deleteObject(oldKey);
        assertEquals(newKey, category.getThumbnail());
        assertEquals(signedUrl, response.getThumbnail());
    }

    @Test
    void deleteCategory_DeletesOldS3Key() {
        String oldUrl = "https://dj1x0wm4k4ps6.cloudfront.net/categories/delete-me.png?Expires=123";
        String rawKey = "categories/delete-me.png";

        Category category = new Category();
        category.setId(1L);
        category.setThumbnail(oldUrl);
        category.setIsActive(true);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(awsS3FileService.extractS3Key(oldUrl)).thenReturn(rawKey);

        categoryService.deleteCategory(1L);

        verify(awsS3FileService, times(1)).deleteObject(rawKey);
        assertFalse(category.getIsActive());
        assertNull(category.getThumbnail());
    }

    @Test
    void getCategory_AnonymousAccess_ResolvesThumbnailWithoutSecurityException() {
        String rawKey = "categories/phone.png";
        String signedUrl = "https://dj1x0wm4k4ps6.cloudfront.net/categories/phone.png?Expires=999&Signature=xyz";

        Category category = new Category();
        category.setId(10L);
        category.setThumbnail(rawKey);

        CategoryResponse mappedResponse =
                CategoryResponse.builder().id(10L).thumbnail(signedUrl).build();

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(categoryMapper.toCategoryResponse(category, awsS3FileService)).thenReturn(mappedResponse);

        CategoryResponse result = categoryService.getCategory(10L);

        assertNotNull(result);
        assertEquals(signedUrl, result.getThumbnail());
    }

    @Test
    void getCategories_AnonymousAccess_ResolvesThumbnailPage() {
        String rawKey = "categories/phone.png";
        String signedUrl = "https://dj1x0wm4k4ps6.cloudfront.net/categories/phone.png?Expires=999&Signature=xyz";

        Category category = new Category();
        category.setId(10L);
        category.setThumbnail(rawKey);

        CategoryResponse mappedResponse =
                CategoryResponse.builder().id(10L).thumbnail(signedUrl).build();

        Page<Category> categoryPage = new PageImpl<>(List.of(category));
        CategorySearchRequest searchRequest = new CategorySearchRequest();

        when(categoryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(categoryPage);
        when(categoryMapper.toCategoryResponse(category, awsS3FileService)).thenReturn(mappedResponse);

        Page<CategoryResponse> pageResult = categoryService.getCategories(searchRequest);

        assertNotNull(pageResult);
        assertEquals(1, pageResult.getContent().size());
        assertEquals(signedUrl, pageResult.getContent().get(0).getThumbnail());
    }
}
