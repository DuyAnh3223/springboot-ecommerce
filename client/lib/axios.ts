// Use in Server Action / Route Handler

import axios from 'axios'

export const serverApi = axios.create({
    baseURL: process.env.SPRING_API_URL,
    timeout: 10000,
})