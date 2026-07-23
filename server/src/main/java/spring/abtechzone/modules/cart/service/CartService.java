package spring.abtechzone.modules.cart.service;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
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

    // ────────────────────────────────────────────────────────
    // POST /cart/add — Thêm sản phẩm vào giỏ
    // ────────────────────────────────────────────────────────
    @Transactional
    public CartResponse addToCart(CartItemRequest request) {
        User user = getAuthenticatedUser();

        // Tìm ProductSku
        ProductSku productSku = productSkuRepository
                .findById(request.getProductSkuId())
                .orElseThrow(() -> new AppException(ErrorCode.SKU_NOT_FOUND));

        // Tìm hoặc tạo Cart cho user
        Cart cart = cartRepository
                .findFirstByUserIdAndStatusOrderByIdDesc(user.getId(), CartStatus.ACTIVE)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .status(CartStatus.ACTIVE)
                            .items(new ArrayList<>())
                            .build();
                    return cartRepository.save(newCart);
                });

        // Kiểm tra xem ProductSku đã có trong giỏ hàng chưa
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductSku().getId().equals(productSku.getId()))
                .findFirst();

        if (existingItem.isPresent()) {
            // Đã tồn tại → cộng dồn số lượng, cập nhật giá mới nhất
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            item.setUnitPrice(productSku.getPrice());
            cartItemRepository.save(item);
        } else {
            // Chưa tồn tại → tạo CartItem mới
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
    @Transactional(readOnly = true)
    public CartResponse getCart() {
        User user = getAuthenticatedUser();

        Cart cart = cartRepository
                .findFirstByUserIdAndStatusOrderByIdDesc(user.getId(), CartStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        // Sync giá mới nhất từ ProductSku cho mỗi item
        for (CartItem item : cart.getItems()) {
            ProductSku sku = item.getProductSku();
            item.setUnitPrice(sku.getPrice());
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
                .findFirstByUserIdAndStatusOrderByIdDesc(user.getId(), CartStatus.ACTIVE)
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
                .findFirstByUserIdAndStatusOrderByIdDesc(user.getId(), CartStatus.ACTIVE)
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
                .findFirstByUserIdAndStatusOrderByIdDesc(user.getId(), CartStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        cart.getItems().clear();
        cartRepository.save(cart);
    }

    // ────────────────────────────────────────────────────────
    // Helper: Lấy User từ SecurityContext
    // ────────────────────────────────────────────────────────
    private User getAuthenticatedUser() {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
