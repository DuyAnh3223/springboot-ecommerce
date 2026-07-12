'use client'

import React, { useEffect, useState } from 'react'
import Link from 'next/link'
import { useAuthStore } from '@/stores/auth-store'
import { LogoutButton } from '@/features/auth/components/signout-button'
import { UserCheck } from 'lucide-react'

const SignIn = () => {
  const { user, isAuthenticated } = useAuthStore()
  const [mounted, setMounted] = useState(false)

  // Avoid hydration mismatch since Zustand store starts as null on the first render
  useEffect(() => {
    setMounted(true)
  }, [])

  if (!mounted) {
    return (
      <Link href="/auth" className="text-sm font-semibold hover:text-shop_orange hover:cursor-pointer hoverEffect">
        Sign In
      </Link>
    )
  }

  if (isAuthenticated && user) {
    const displayName = user.firstName 
      ? `${user.lastName || ''} ${user.firstName}`.trim() 
      : user.username;

    return (
      <div className="flex items-center gap-3 animate-in fade-in duration-300">
        <div className="flex items-center gap-1.5 text-sm text-shop_dark_green font-semibold">
          <UserCheck className="size-4 text-shop_light_green" />
          <span>Chào, {displayName}</span>
        </div>
        <LogoutButton />
      </div>
    )
  }

  return (
    <Link href="/auth" className="text-sm font-semibold hover:text-shop_orange hover:cursor-pointer hoverEffect">
      Sign In
    </Link>
  )
}

export default SignIn