package spring.abtechzone.modules.cart.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.cart.constant.CartStatus;
import spring.abtechzone.modules.cart.dto.request.CartItemRequest;
import spring.abtechzone.modules.cart.dto.response.CartItemRespone;
import spring.abtechzone.modules.cart.dto.response.CartResponse;
import spring.abtechzone.modules.cart.entity.Cart;
import spring.abtechzone.modules.cart.entity.CartItem;
import spring.abtechzone.modules.cart.mapper.CartItemMapper;
import spring.abtechzone.modules.cart.repository.CartItemRepository;
import spring.abtechzone.modules.cart.repository.CartRepository;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CartSevice {

    UserRepository userRepository;
    CartRepository cartRepository;
    CartItemRepository cartItemRepository;
    ProductSkuRepository productSkuRepository;
    private final CartItemMapper cartItemMapper;

    @Transactional
    public CartResponse addToCart(String userId, CartItemRequest cartItemRequest) {
        // 1. Tìm user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 2. Tìm ProductSku
        ProductSku productSku = productSkuRepository.findById(cartItemRequest.getProductSkuId())
                .orElseThrow(() -> new AppException(ErrorCode.SKU_NOT_FOUND));

        // 3. Tìm hoặc tạo Cart cho user
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .status(CartStatus.ACTIVE)
                            .items(new ArrayList<>())
                            .build();
                    return cartRepository.save(newCart);
                });

        // 4. Kiểm tra xem ProductSku đã có trong giỏ hàng chưa
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductSku().getId().equals(productSku.getId()))
                .findFirst();

        if (existingItem.isPresent()) {
            // Nếu đã tồn tại -> cập nhật số lượng
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + cartItemRequest.getQuantity());
            item.setUnitPrice(productSku.getPrice());
            cartItemRepository.save(item);
        } else {
            // Nếu chưa tồn tại -> tạo CartItem mới
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .productSku(productSku)
                    .quantity(cartItemRequest.getQuantity())
                    .unitPrice(productSku.getPrice())
                    .build();
            cartItemRepository.save(newItem);
            cart.getItems().add(newItem);
        }

        return toCartResponse(cart);
    }

    @Transactional
    public CartItemRespone updateCartItemQuantity(Long cartItemId, int delta) {
        // 1. Tìm CartItem
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        int newQuantity = cartItem.getQuantity() + delta;

        // 2. Số lượng tối thiểu là 1
        if (newQuantity < 1) {
            throw new AppException(ErrorCode.CART_ITEM_QUANTITY_INVALID);
        }

        // 3. Kiểm tra tồn kho
        int stock = cartItem.getProductSku().getStock();
        if (newQuantity > stock) {
            throw new AppException(ErrorCode.PRODUCT_STOCK_INVALID);
        }

        // 4. Cập nhật số lượng
        cartItem.setQuantity(newQuantity);
        cartItem = cartItemRepository.save(cartItem);

        // 5. Trả về response
        return CartItemRespone.builder()
                .cartId(cartItem.getCart().getId())
                .quantity(cartItem.getQuantity())
                .unitPrice(cartItem.getUnitPrice())
                .productName(cartItem.getProductSku().getProduct().getName())
                .build();
    }

    @Transactional
    public void removeCartItem(Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));
        cartItemRepository.delete(cartItem);
    }

    public CartResponse getCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return toCartResponse(cart);
    }

    private CartResponse toCartResponse(Cart cart) {
        List<CartItemRespone> itemResponses = cart.getItems().stream()
                .map(item -> CartItemRespone.builder()
                        .cartId(cart.getId())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .productName(item.getProductSku().getProduct().getName())
                        .build())
                .collect(Collectors.toList());

        return CartResponse.builder()
                .cartStatus(cart.getStatus())
                .items(itemResponses)
                .userId(cart.getUser().getId())
                .build();
    }
}
