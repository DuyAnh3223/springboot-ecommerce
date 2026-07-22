import http from "k6/http";
import { check, sleep } from "k6";

const TOKEN = __ENV.TOKEN || "eyJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJ4eHguY29tIiwic3ViIjoidXNlcjEiLCJleHAiOjE3ODQ3ODc1NTYsImlhdCI6MTc4NDcwMTE1NiwianRpIjoiOThlNDkwNDktNzgyNC00MTRhLTgxZGQtMGFhZjBiYzA4ZTlhIiwic2NvcGUiOiJST0xFX1VTRVIifQ.kaL0Z1w_rRwKBDzZQDOgX5fmWjr_yfypma-nrhLDN8DZQO1x8AGsPGpEry1fKxHvWGMqd_NlCeXPCpzFDkbDSg";
const BASE_URL = __ENV.BASE_URL || "http://localhost:8080/abtechzone";
const SKU_ID = __ENV.SKU_ID || 3;

export const options = {
    scenarios: {
        create_order_test: {
            executor: "ramping-vus",
            stages: [
                { duration: "10s", target: 10 },
                { duration: "20s", target: 30 },
                { duration: "20s", target: 50 },
                { duration: "10s", target: 0 }
            ]
        }
    },
    thresholds: {
        http_req_failed: ["rate<0.10"],
        http_req_duration: ["p(95)<1000"]
    }
};

export default function () {
    const headers = {
        Authorization: `Bearer ${TOKEN}`,
        "Content-Type": "application/json"
    };

    // Bước 1: Đảm bảo giỏ hàng có sản phẩm trước khi tạo đơn
    const addCartRes = http.post(
        `${BASE_URL}/cart/add`,
        JSON.stringify({
            productSkuId: Number(SKU_ID),
            quantity: 1
        }),
        { headers }
    );

    // Bước 2: Thực hiện POST /orders
    const payload = JSON.stringify({
        newUserAddress: {
            recipientName: "Nguyen Van Test",
            phone: "0987654321",
            province: "Ha Noi",
            ward: "Dich Vong",
            street: "123 Xuan Thuy",
            saveAddress: false
        },
        paymentMethod: "COD"
    });

    const orderRes = http.post(`${BASE_URL}/orders`, payload, { headers });

    // Kiểm tra kết quả tạo đơn hàng thành công HTTP 200
    const isSuccess = check(orderRes, {
        "order created successfully (status 200)": (r) => r.status === 200
    });

    if (!isSuccess && __ITER < 3) {
        console.log(`[POST /orders Error ${orderRes.status}]: ${orderRes.body}`);
    }

    // Nghỉ 50ms giữa mỗi vòng lặp để Redisson giải phóng lock:user-order
    sleep(0.05);
}
