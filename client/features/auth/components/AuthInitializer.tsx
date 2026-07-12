'use client'

import { useRef } from 'react'
import { useAuthStore } from '@/stores/auth-store'

interface User {
  username: string
  firstName?: string
  lastName?: string
  roles?: Array<{ name: string }>
}

export default function AuthInitializer({
  user,
  children,
}: {
  user: User | null
  children: React.ReactNode
}) {
  const initialized = useRef(false)
  
  if (!initialized.current) {
    useAuthStore.setState({ user, isAuthenticated: !!user })
    initialized.current = true
  }

  return <>{children}</>
}
