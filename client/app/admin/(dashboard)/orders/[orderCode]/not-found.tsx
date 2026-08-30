import Link from "next/link";
import { Button } from "@/components/ui/button";

export default function NotFound() { return <div className="space-y-4 py-12 text-center"><h1 className="text-2xl font-semibold">Không tìm thấy đơn hàng</h1><p className="text-muted-foreground">Đơn hàng có thể đã bị xóa hoặc mã không chính xác.</p><Button render={<Link href="/admin/orders" />}>Về danh sách đơn hàng</Button></div>; }
