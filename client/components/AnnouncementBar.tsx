"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Flame, ArrowRight } from "lucide-react";

export default function AnnouncementBar() {
  const [timeLeft, setTimeLeft] = useState({
    hours: 11,
    minutes: 45,
    seconds: 30,
  });

  useEffect(() => {
    const timer = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev.seconds > 0) {
          return { ...prev, seconds: prev.seconds - 1 };
        }
        if (prev.minutes > 0) {
          return { ...prev, minutes: prev.minutes - 1, seconds: 59 };
        }
        if (prev.hours > 0) {
          return { hours: prev.hours - 1, minutes: 59, seconds: 59 };
        }
        return prev;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  const formatNum = (num: number) => String(num).padStart(2, "0");

  return (
    <div className="bg-gradient-to-r from-slate-900 via-rose-950 to-slate-900 text-white text-xs py-2 px-4 border-b border-rose-900/30">
      <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2 font-medium">
          <span className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-rose-600 text-[10px] font-bold uppercase tracking-wider animate-pulse">
            <Flame className="w-3 h-3 text-yellow-300 fill-yellow-300" />
            Flash Sale
          </span>
          <span className="hidden sm:inline text-slate-200">
            Giảm tới 50% linh kiện PC & Gaming Rig chính hãng!
          </span>
        </div>

        <div className="flex items-center gap-3 ml-auto sm:ml-0">
          <div className="flex items-center gap-1 font-mono text-[11px]">
            <span className="bg-rose-900/80 border border-rose-700/50 px-1.5 py-0.5 rounded text-rose-200 font-bold">
              {formatNum(timeLeft.hours)}
            </span>
            <span className="text-rose-400 font-bold">:</span>
            <span className="bg-rose-900/80 border border-rose-700/50 px-1.5 py-0.5 rounded text-rose-200 font-bold">
              {formatNum(timeLeft.minutes)}
            </span>
            <span className="text-rose-400 font-bold">:</span>
            <span className="bg-rose-900/80 border border-rose-700/50 px-1.5 py-0.5 rounded text-rose-200 font-bold">
              {formatNum(timeLeft.seconds)}
            </span>
          </div>

          <Link
            href="#flash-sale"
            className="inline-flex items-center gap-1 text-rose-300 hover:text-white font-semibold hover:underline text-[11px] transition-colors"
          >
            Săn deal ngay <ArrowRight className="w-3 h-3" />
          </Link>
        </div>
      </div>
    </div>
  );
}
