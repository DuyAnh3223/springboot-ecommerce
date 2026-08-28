import { CircleCheckBig, Clock3 } from "lucide-react";
import type { OrderHistoryResponse } from "@/features/orders/order.type";
import {
  ORDER_STATUS_META,
  formatOrderDate,
  getHistoryTargetStatus,
} from "../utils/order.utils";

interface OrderTimelineProps {
  history: OrderHistoryResponse[];
}

function getActorLabel(actorType: string | null): string {
  switch (actorType) {
    case "CUSTOMER":
      return "Khách hàng";
    case "ADMIN":
      return "ABTechZone";
    case "SYSTEM":
      return "Hệ thống";
    default:
      return "Hệ thống";
  }
}

export function OrderTimeline({ history }: OrderTimelineProps) {
  if (history.length === 0) {
    return (
      <p className="rounded-xl bg-slate-50 p-4 text-sm text-slate-500">
        Lịch sử trạng thái đang được cập nhật.
      </p>
    );
  }

  return (
    <ol className="space-y-0" aria-label="Lịch sử trạng thái đơn hàng">
      {history.map((entry, index) => {
        const targetStatus = getHistoryTargetStatus(entry);
        const metadata = targetStatus ? ORDER_STATUS_META[targetStatus] : null;
        const isLatest = index === history.length - 1;

        return (
          <li
            key={`${entry.createdAt}-${index}`}
            className="relative grid grid-cols-[28px_minmax(0,1fr)] gap-3 pb-6 last:pb-0"
          >
            {index < history.length - 1 && (
              <span className="absolute left-[13px] top-7 h-[calc(100%-20px)] w-px bg-slate-200" />
            )}
            <span className="relative z-10 flex h-7 w-7 items-center justify-center rounded-full bg-white text-shop_light_green ring-2 ring-emerald-100">
              {isLatest ? <CircleCheckBig className="h-4 w-4" /> : <Clock3 className="h-3.5 w-3.5" />}
            </span>
            <div>
              <p className="font-bold text-slate-800">
                {metadata?.label ?? "Cập nhật đơn hàng"}
              </p>
              <p className="mt-1 text-xs text-slate-500">
                {formatOrderDate(entry.createdAt)} · {getActorLabel(entry.actorType)}
              </p>
              {entry.note && (
                <p className="mt-2 rounded-lg bg-slate-50 px-3 py-2 text-sm text-slate-600">
                  {entry.note}
                </p>
              )}
            </div>
          </li>
        );
      })}
    </ol>
  );
}
