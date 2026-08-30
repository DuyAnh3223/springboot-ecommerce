"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { useAsyncAction } from "@/shared/hooks/useAsyncAction";
import type { OrderStatus } from "@/features/orders/order.type";
import { updateAdminOrderStatusAction } from "../actions/admin-order.action";
import { getAllowedStatusActions, normalizeAdminNote } from "../utils/admin-order.utils";

export function AdminOrderStatusActions({ orderCode, allowedTransitions }: { orderCode: string; allowedTransitions: OrderStatus[] }) {
  const [selected, setSelected] = useState<OrderStatus | null>(null);
  const [note, setNote] = useState("");
  const router = useRouter();
  const { isLoading, error, setError, run } = useAsyncAction();
  const actions = getAllowedStatusActions(allowedTransitions);
  if (!actions.length && !error) return null;
  async function submit() {
    if (!selected) return;
    const result = await run(() => updateAdminOrderStatusAction(orderCode, { status: selected, note: normalizeAdminNote(note) }));
    if (result?.ok) { setSelected(null); setNote(""); router.refresh(); }
    else if (result) { setError(result.error); if (result.refresh) router.refresh(); }
  }
  return <div className="space-y-3 rounded-lg border p-4"><h2 className="font-semibold">Cập nhật trạng thái</h2><div className="flex flex-wrap gap-2">{actions.map((action) => <Button key={action.status} type="button" variant={action.status === "CANCELLED" ? "destructive" : "default"} disabled={isLoading} onClick={() => setSelected(action.status)}>{action.label}</Button>)}</div>{selected && <div className="space-y-2"><p className="text-sm">Ghi chú {selected === "CANCELLED" ? "(bắt buộc lý do hủy)" : "(không bắt buộc)"}</p><Textarea value={note} maxLength={500} onChange={(event) => setNote(event.target.value)} placeholder="Nhập ghi chú tối đa 500 ký tự" /><div className="flex gap-2"><Button type="button" disabled={isLoading || (selected === "CANCELLED" && !note.trim())} onClick={submit}>{isLoading ? "Đang cập nhật..." : "Xác nhận"}</Button><Button type="button" variant="outline" disabled={isLoading} onClick={() => setSelected(null)}>Đóng</Button></div></div>}{error && <p className="text-sm text-destructive">{error}</p>}</div>;
}
