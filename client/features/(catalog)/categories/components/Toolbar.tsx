import React from 'react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Search, SlidersHorizontal, Download, Filter } from 'lucide-react';

interface ToolbarProps {
  searchTerm: string;
  onSearchChange: (value: string) => void;
}

const Toolbar = ({
  searchTerm,
  onSearchChange,
}: ToolbarProps) => {
  return (
    <div className="w-full py-1">
      <div className="flex flex-col md:flex-row gap-3 items-center justify-between">
        {/* Left Side: Search and Filter Button */}
        <div className="flex items-center gap-2 w-full md:max-w-md">
          <div className="relative flex-1">
            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">
              <Search className="size-4" />
            </span>
            <Input
              type="text"
              value={searchTerm}
              onChange={(e) => onSearchChange(e.target.value)}
              placeholder="Search categories..."
              className="pl-9 h-7 border-slate-200 focus-visible:ring-slate-400/20 focus-visible:border-slate-300 w-full bg-white"
            />
          </div>
        </div>

        {/* Right Side: Columns and Export Buttons */}
        <div className="flex items-center gap-2 w-full md:w-auto justify-end">
          <Button
            variant="outline"
            className="h-7 border-slate-200 text-slate-600 hover:text-slate-850 hover:bg-slate-50 gap-1.5 cursor-pointer font-medium px-3 rounded-lg bg-white"
          >
            <Download className="size-4" />
            Export
          </Button>
        </div>
      </div>
    </div>
  );
};

export default Toolbar;