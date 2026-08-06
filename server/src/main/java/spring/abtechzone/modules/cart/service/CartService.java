package spring.abtechzone.modules.cart.service;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.auth.service.AuthService;
import spring.abtechzone.modules.cart.constant.CartStatus;
import spring.abtechzone.modules.cart.dto.request.CartItemRequest;
import spring.abtechzone.modules.cart.dto.request.UpdateQuantityRequest;
import spring.abtechzone.modules.cart.dto.response.CartItemResponse;
import spring.abtechzone.modules.cart.dto.response.CartResponse;
import spring.abtechzone.modules.cart.entity.Cart;
import spring.abtechzone.modules.cart.entity.CartItem;
import spring.abtechzone.modules.cart.mapper.CartItemMapper;
import spring.abtechzone.modules.cart.mapper.CartMapper;
import spring.abtechzone.modules.cart.repository.CartItemRepository;
import spring.abtechzone.modules.cart.repository.CartRepository;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.repository.UserRepository;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CartService {

    UserRepository userRepository;
    CartRepository cartRepository;
    CartItemRepository cartItemRepository;
    ProductSkuRepository productSkuRepository;
    CartItemMapper cartItemMapper;
    CartMapper cartMapper;
    AuthService authService;

    // ────────────────────────────────────────────────────────
    // POST /cart/add — Thêm sản phẩm vào giỏ
    // ────────────────────────────────────────────────────────
    @Transactional
    public CartResponse addToCart(CartItemRequest request) {
        if (request == null || request.getQuantity() == null || request.getQuantity() < 1) {
            throw new AppException(ErrorCode.CART_ITEM_QUANTITY_INVALID);
        }

        if (request.getProductSkuId() == null) {
            throw new AppException(ErrorCode.SKU_NOT_FOUND);
        }

        // Tìm ProductSku
        ProductSku productSku = productSkuRepository
                .findById(request.getProductSkuId())
                .orElseThrow(() -> new AppException(ErrorCode.SKU_NOT_FOUND));

        User user = getAuthenticatedUser();

        int stock = productSku.getStock() != null ? productSku.getStock() : 0;

        // Validation 1: Request quantity must be <= stock
        if (request.getQuantity() > stock) {
            throw new AppException(ErrorCode.PRODUCT_STOCK_INVALID);
        }

        // Validation 2: Cumulative quantity must be <= stock
        Optional<Cart> existingCartOpt = cartRepository.findByUserIdAndStatus(user.getId(), CartStatus.ACTIVE);
        Optional<CartItem> existingItem = Optional.empty();
        if (existingCartOpt.isPresent()) {
            existingItem = existingCartOpt.get().getItems().stream()
                    .filter(item -> item.getProductSku().getId().equals(productSku.getId()))
                    .findFirst();
        }

        if (existingItem.isPresent()) {
            long newQuantity = (long) existingItem.get().getQuantity() + request.getQuantity();
            if (newQuantity > stock) {
                throw new AppException(ErrorCode.PRODUCT_STOCK_INVALID);
            }
        }

        // Cả hai validation đều đã pass → tiến hành lấy/tạo Cart và lưu Item
        Cart cart = existingCartOpt.orElseGet(() -> {
            Cart newCart = Cart.builder()
                    .user(user)
                    .status(CartStatus.ACTIVE)
                    .items(new ArrayList<>())
                    .build();
            return cartRepository.save(newCart);
        });

        // Re-check item trong giỏ (có thể là cart cũ hoặc vừa tạo)
        Optional<CartItem> itemInCartOpt = cart.getItems().stream()
                .filter(item -> item.getProductSku().getId().equals(productSku.getId()))
                .findFirst();

        if (itemInCartOpt.isPresent()) {
            CartItem item = itemInCartOpt.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            item.setUnitPrice(productSku.getPrice());
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .productSku(productSku)
                    .quantity(request.getQuantity())
                    .unitPrice(productSku.getPrice())
                    .build();
            cartItemRepository.save(newItem);
            cart.getItems().add(newItem);
        }

        return cartMapper.toCartResponse(cart);
    }

    // ────────────────────────────────────────────────────────
    // GET /cart — Lấy giỏ hàng (giá & trạng thái mới nhất)
    // ────────────────────────────────────────────────────────
    @Transactional
    public CartResponse getCart() {
        User user = getAuthenticatedUser();

        Cart cart = cartRepository
                .findByUserIdAndStatus(user.getId(), CartStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        // Sync giá mới nhất từ ProductSku cho mỗi item
        boolean priceChanged = false;
        for (CartItem item : cart.getItems()) {
            ProductSku sku = item.getProductSku();
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(sku.getPrice()) != 0) {
                item.setUnitPrice(sku.getPrice());
                priceChanged = true;
            }
        }

        if (priceChanged) {
            cartRepository.save(cart);
        }

        return cartMapper.toCartResponse(cart);
    }

    // ────────────────────────────────────────────────────────
    // DELETE /cart/items/:skuId — Xoá 1 item khỏi giỏ
    // ────────────────────────────────────────────────────────
    @Transactional
    public void removeCartItem(Long skuId) {
        User user = getAuthenticatedUser();

        Cart cart = cartRepository
                .findByUserIdAndStatus(user.getId(), CartStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        CartItem cartItem = cartItemRepository
                .findByCartIdAndProductSkuId(cart.getId(), skuId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        cartItemRepository.delete(cartItem);
    }

    // ────────────────────────────────────────────────────────
    // PATCH /cart/items/:skuId — Cập nhật số lượng (absolute)
    // ────────────────────────────────────────────────────────
    @Transactional
    public CartItemResponse updateCartItemQuantity(Long skuId, UpdateQuantityRequest request) {
        User user = getAuthenticatedUser();

        Cart cart = cartRepository
                .findByUserIdAndStatus(user.getId(), CartStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        CartItem cartItem = cartItemRepository
                .findByCartIdAndProductSkuId(cart.getId(), skuId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        int newQuantity = request.getQuantity();

        // Kiểm tra tồn kho
        int stock = cartItem.getProductSku().getStock();
        if (newQuantity > stock) {
            throw new AppException(ErrorCode.PRODUCT_STOCK_INVALID);
        }

        // Cập nhật số lượng & sync giá mới nhất
        cartItem.setQuantity(newQuantity);
        cartItem.setUnitPrice(cartItem.getProductSku().getPrice());
        cartItem = cartItemRepository.save(cartItem);

        return cartItemMapper.toCartItemResponse(cartItem);
    }

    // ────────────────────────────────────────────────────────
    // DELETE /cart — Xoá toàn bộ giỏ hàng
    // ────────────────────────────────────────────────────────
    @Transactional
    public void clearCart() {
        User user = getAuthenticatedUser();

        Cart cart = cartRepository
                .findByUserIdAndStatus(user.getId(), CartStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        cart.getItems().clear();
        cartRepository.save(cart);
    }

    // ────────────────────────────────────────────────────────
    // Helper: Lấy User từ SecurityContext
    // ────────────────────────────────────────────────────────
    private User getAuthenticatedUser() {
        String username = authService.getCurrentUsername();
        return userRepository.findByUsername(username).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
