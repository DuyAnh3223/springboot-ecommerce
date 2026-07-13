'use client'

import { signoutAction } from '@/features/auth/actions'
import { useAuthStore } from '@/features/auth/stores/auth.store'
import { useRouter } from 'next/navigation'
import { Button } from '@/components/ui/button'
import { LogOut, Loader2 } from 'lucide-react'
import { useState } from 'react'

export function LogoutButton() {
  const router = useRouter()
  const clear = useAuthStore((s) => s.clear)
  const [loading, setLoading] = useState(false)

  return (
    <Button
      variant="outline"
      size="sm"
      disabled={loading}
      className="text-destructive hover:bg-destructive/10 border-destructive/20 hover:text-destructive text-xs h-8 cursor-pointer gap-1.5"
      onClick={async () => {
        setLoading(true)
        try {
          await signoutAction()
          clear()
          router.push('/sign-in')
          router.refresh()
        } catch (error) {
          console.error(error)
        } finally {
          setLoading(false)
        }
      }}
    >
      {loading ? (
        <Loader2 className="size-3 animate-spin" />
      ) : (
        <LogOut className="size-3" />
      )}
      Đăng xuất
    </Button>
  )
}