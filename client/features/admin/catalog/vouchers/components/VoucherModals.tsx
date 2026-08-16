import React from "react";
import { VoucherFormDialog } from "./VoucherFormDialog";
import { VoucherToggleActiveDialog } from "./VoucherToggleActiveDialog";

export function VoucherModals() {
  return (
    <>
      <VoucherFormDialog />
      <VoucherToggleActiveDialog />
    </>
  );
}
