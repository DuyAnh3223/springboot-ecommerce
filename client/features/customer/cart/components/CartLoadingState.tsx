"use client";

import React from "react";

export function CartLoadingState() {
  return (
    <div className="animate-pulse space-y-6">
      <div className="h-8 w-48 rounded-lg bg-slate-200" />
      <div className="grid grid-cols-1 gap-8 lg:grid-cols-12">
        <div className="space-y-4 lg:col-span-8">
          <div className="h-14 rounded-xl bg-slate-100" />
          <div className="h-28 rounded-xl bg-slate-100" />
          <div className="h-28 rounded-xl bg-slate-100" />
        </div>
        <div className="lg:col-span-4">
          <div className="h-64 rounded-2xl bg-slate-100" />
        </div>
      </div>
    </div>
  );
}
