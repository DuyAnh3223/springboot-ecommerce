# Unit Testing Guidelines & Mockito Conventions

> **Verified against commit `5e44606` on 2026-07-25**

## 1. Unit Test Architecture & Setup

All service layer unit tests follow JUnit 5 with Mockito Extension (`@ExtendWith(MockitoExtension.class)`).

```java
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    CartRepository cartRepository;

    @Mock
    CartItemRepository cartItemRepository;

    @Mock
    ProductSkuRepository productSkuRepository;

    @Mock
    CartItemMapper cartItemMapper;

    @Mock
    CartMapper cartMapper;

    @Mock
    AuthService authService;

    @InjectMocks
    CartService cartService;
}
```

---

## 2. Mocking Guidelines & Rationale

### 2.1 AuthService Mocking & `lenient()` Rationale
When testing services that depend on user authentication (`CartService`, `OrderService`, `UserService`), inject `@Mock AuthService authService;`.

**Why use `lenient()` for `authService` in `@BeforeEach`?**
```java
@BeforeEach
void setUp() {
    SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("testuser", null, List.of()));
    lenient().when(authService.getCurrentUsername()).thenReturn("testuser");
}
```
- **Rationale**: Shared `@BeforeEach` setup methods configure `authService.getCurrentUsername()` for all nested unit tests. Some edge-case tests (e.g. testing missing parameters or precondition failures) may fail or throw an exception before reaching the `authService.getCurrentUsername()` call. Strict Mockito stubbing without `lenient()` would trigger `org.mockito.exceptions.misusing.UnnecessaryStubbingException` and fail the build. Using `lenient()` allows safe shared test setup.

### 2.2 MapStruct Mapper Stubbing
For unit tests testing complex entity-to-DTO transformations (e.g. `OrderServiceTest`), prefer `@Spy` with MapStruct factory over full `@Mock`:

```java
@Spy
OrderMapper orderMapper = Mappers.getMapper(spring.abtechzone.modules.order.mapper.OrderMapper.class);
```
- **Rationale**: `@Spy` allows real MapStruct mapping logic to execute without tedious manual mock stubbing for every DTO field, ensuring real mapping behavior is validated.

### 2.3 Exact Repository Method Matching
Always stub the exact repository method invoked by the service implementation:
- Correct: `when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));`
- Incorrect: Stubbing a different or legacy method name (e.g. `findFirstByUserIdAndStatusOrderByIdDesc`) which causes `Cart not found` or `UnnecessaryStubbingException`.

---

## 3. Standard Test Template Example

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock UserRepository userRepository;
    @Mock CartRepository cartRepository;
    @Mock VoucherRepository voucherRepository;
    @Mock ProductSkuRepository productSkuRepository;
    @Mock OrderRepository orderRepository;
    @Mock AddressRepository addressRepository;
    @Mock VoucherValidator voucherValidator;
    @Mock InventoryService inventoryService;
    @Mock OrderStatusHistoryRepository orderStatusHistoryRepository;
    @Mock RedissonClient redissonClient;
    @Mock TransactionTemplate transactionTemplate;
    @Mock AuthService authService;

    @Spy
    OrderMapper orderMapper = Mappers.getMapper(OrderMapper.class);

    @InjectMocks
    OrderService orderService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("testuser", null, List.of()));
        lenient().when(authService.getCurrentUsername()).thenReturn("testuser");
    }

    @Test
    void shouldCreateOrderSuccessfully() {
        // Given
        when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));
        // ...
        
        // When
        OrderResponse response = orderService.createOrder(request);

        // Then
        assertThat(response).isNotNull();
    }
}
```
