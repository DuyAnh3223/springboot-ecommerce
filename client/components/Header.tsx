import React from 'react'
import Container from './Container'
import Logo from './Logo'
import HeaderMenu from './HeaderMenu'
import SearchBar from './SearchBar'
import CartIcon from './CartIcon'
import FavoriteButton from './FavoriteButton'
import SignIn from './SignIn'
import MobileMenu from './MobileMenu'

const Header = () => {
  return (
    <header className='bg-white py-5  '>
      <Container className='flex items-center justify-between text-lightColor' >
        {/*Logo  */}
       <div className='w-auto md:w-1/3 flex items-center gap-2.5 justify-start md:gap-0'>
         <MobileMenu/>
         <Logo/>
       </div>
        {/* NavButton */}
        <HeaderMenu/>
        <div className='w-auto md:h-1/3 flex items-center justify-end gap-5'>
          <SearchBar/>
          <CartIcon/>
          <FavoriteButton/>
          <SignIn/>
        </div>
        {/* NavAdmin */}
      </Container>
    </header>
  )
}

export default Header