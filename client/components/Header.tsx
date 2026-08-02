import React from "react";
import Link from "next/link";
import { Wrench } from "lucide-react";
import Container from "./Container";
import Logo from "./Logo";
import HeaderMenu from "./HeaderMenu";
import SearchBar from "./SearchBar";
import CartIcon from "./CartIcon";
import FavoriteButton from "./FavoriteButton";
import SignIn from "./SignIn";
import MobileMenu from "./MobileMenu";

const Header = () => {
  return (
    <header className="bg-white py-4 border-b border-slate-150 sticky top-0 z-40 shadow-xs">
      <Container className="flex items-center justify-between text-lightColor gap-4">
        {/* Logo */}
        <div className="flex items-center gap-2.5">
          <MobileMenu />
          <Logo />
        </div>

        {/* Navigation Menu */}
        <HeaderMenu />

        {/* Actions & Tools */}
        <div className="flex items-center justify-end gap-3 sm:gap-4">
          <SearchBar />
          <Link
            href="#pc-builder"
            className="hidden lg:inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-900 hover:bg-rose-600 text-white text-xs font-bold transition-all shadow-xs"
          >
            <Wrench className="w-3.5 h-3.5" />
            <span>Build PC</span>
          </Link>
          <CartIcon />
          <FavoriteButton />
          <SignIn />
        </div>
      </Container>
    </header>
  );
};

export default Header;