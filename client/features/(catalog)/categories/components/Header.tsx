import React from 'react';
import { Button } from '@/components/ui/button';
import { Plus } from 'lucide-react';

interface HeaderProps {
  onAdd: () => void;
}

const Header = ({ onAdd }: HeaderProps) => {
  return (
    <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 py-2">
      <div>
        {/* Breadcrumbs */}
        <div className="flex items-center gap-1.5 text-xs text-slate-500 font-medium mb-1.5">
          <span className="hover:text-slate-800 cursor-pointer">Dashboard</span>
          <span className="text-slate-300 font-normal">&gt;</span>
          <span className="text-slate-800 font-semibold">Categories</span>
        </div>

        {/* Title */}
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Categories</h1>
        <p className="text-sm text-slate-500 mt-1">Browse and manage your product categories.</p>
      </div>

      {/* Add Button  */}
      <Button
        onClick={onAdd}
        className="h-10 bg-shop_dark_green hover:bg-shop_btn_dark_green text-white cursor-pointer gap-1.5 font-semibold shadow-sm transition-colors rounded-lg px-4"
      >
        <Plus className="size-4.5" />
        Add Category
      </Button>
    </div>
  );
};

export default Header;