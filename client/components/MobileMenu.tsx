"use client"
import { Button } from '@base-ui/react'
import { AlignLeft } from 'lucide-react'
import React, { useState } from 'react'
import SideMenu from './SideMenu'

const MobileMenu = () => {
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  return (
    <>
    <Button onClick={()=>setIsSidebarOpen(!isSidebarOpen)}>
        <AlignLeft className='hover:text-darkColor hoverEffect md:hidden hover:cursor-pointer'/>    
    </Button>
    <div className='md:hidden'>
        <SideMenu
        isOpen={isSidebarOpen}
        onClose={()=> setIsSidebarOpen(false)} />
    </div>
    
    </>
  )
}

export default MobileMenu