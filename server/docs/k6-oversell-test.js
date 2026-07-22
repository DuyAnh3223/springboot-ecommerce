import http from "k6/http";
import { check } from "k6";

const TOKEN = __ENV.TOKEN || "eyJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJ4eHguY29tIiwic3ViIjoidXNlcjEiLCJleHAiOjE3ODQ3ODc1NTYsImlhdCI6MTc4NDcwMTE1NiwianRpIjoiOThlNDkwNDktNzgyNC00MTRhLTgxZGQtMGFhZjBiYzA4ZTlhIiwic2NvcGUiOiJST0xFX1VTRVIifQ.kaL0Z1w_rRwKBDzZQDOgX5fmWjr_yfypma-nrhLDN8DZQO1x8AGsPGpEry1fKxHvWGMqd_NlCeXPCpzFDkbDSg";
const BASE_URL = __ENV.BASE_URL || "http://localhost:8080/abtechzone";
const SKU_ID = __ENV.SKU_ID || 4;

export const options = {
    scenarios: {
        oversell_test: {
            executor: "per-vu-iterations",
            vus: 50,
            iterations: 1,
            maxDuration: "15s"
        }
    }
};

export default function () {
    const headers = {
        Authorization: `Bearer ${TOKEN}`,
        "Content-Type": "application/json"
    };

    // 1. Thêm sản phẩm SKU vào giỏ trước khi tạo đơn
    http.post(
        `${BASE_URL}/cart/add`,
        JSON.stringify({
            productSkuId: Number(SKU_ID),
            quantity: 1
        }),
        { headers }
    );

    // 2. Bắn request tạo đơn hàng
    const payload = JSON.stringify({
        newUserAddress: {
            recipientName: "Test Oversell",
            phone: "0912345678",
            province: "Ha Noi",
            ward: "Dich Vong",
            street: "123 Xuan Thuy",
            saveAddress: false
        },
        paymentMethod: "COD"
    });

    const res = http.post(`${BASE_URL}/orders`, payload, { headers });

    check(res, {
        "Status 200 (Success Order)": (r) => r.status === 200,
        "Status 400 (Blocked by Lock/Stock)": (r) => r.status === 400
    });
}
