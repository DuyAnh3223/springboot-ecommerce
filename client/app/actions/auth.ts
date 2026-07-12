'use server'

import { cookies } from 'next/headers'
import { serverApi } from '@/lib/axios'
import { SignInInput, SignUpInput } from '@/schemas/auth'

export async function signInAction(values: SignInInput) {
  try {
    const response = await serverApi.post('/auth/log-in', {
      username: values.username,
      password: values.password,
    })

    const apiResponse = response.data

    if (apiResponse && apiResponse.code === 1000 && apiResponse.result?.token) {
      const { token } = apiResponse.result
      
      const cookieStore = await cookies()
      cookieStore.set('token', token, {
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'lax',
        maxAge: 60 * 60 * 24 * 7, // 7 days
        path: '/',
      })

      // Fetch current user info
      const userResponse = await serverApi.get('/users/my-info', {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })

      if (userResponse.data && userResponse.data.code === 1000) {
        return {
          success: true,
          user: userResponse.data.result,
        }
      }

      return {
        success: true,
        user: { username: values.username },
      }
    }

    return { error: apiResponse?.message || 'Đăng nhập không thành công' }
  } catch (error: any) {
    console.error('Sign in action error:', error)
    const backendMessage = error.response?.data?.message
    return { error: backendMessage || 'Đăng nhập không thành công. Vui lòng kiểm tra lại tài khoản hoặc kết nối mạng.' }
  }
}

export async function signUpAction(values: SignUpInput) {
  try {
    const response = await serverApi.post('/users', {
      username: values.username,
      password: values.password,
      firstName: values.firstName,
      lastName: values.lastName,
    })

    const apiResponse = response.data

    if (apiResponse && apiResponse.code === 1000) {
      return { success: true }
    }

    return { error: apiResponse?.message || 'Đăng ký không thành công' }
  } catch (error: any) {
    console.error('Sign up action error:', error)
    const backendMessage = error.response?.data?.message
    if (error.response?.data?.code === 1001) {
      return { error: 'Tài khoản đã tồn tại' }
    }
    return { error: backendMessage || 'Đăng ký không thành công. Vui lòng thử lại.' }
  }
}

export async function signoutAction() {
  try {
    const cookieStore = await cookies()
    const token = cookieStore.get('token')?.value

    if (token) {
      try {
        await serverApi.post('/auth/log-out', { token })
      } catch (backendError) {
        console.error('Error logging out from backend:', backendError)
      }
    }

    cookieStore.delete('token')
    return { success: true }
  } catch (error) {
    console.error('Sign out action error:', error)
    return { error: 'Đăng xuất không thành công' }
  }
}

export async function getSession() {
  try {
    const cookieStore = await cookies()
    const token = cookieStore.get('token')?.value

    if (!token) return null

    const userResponse = await serverApi.get('/users/my-info', {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })

    if (userResponse.data && userResponse.data.code === 1000) {
      return userResponse.data.result
    }

    return null
  } catch (error) {
    console.error('Get session error:', error)
    return null
  }
}
