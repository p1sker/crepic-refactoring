import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react-swc' // (질문자님은 SWC를 선택하셨으니 이게 맞을 거예요!)
import tailwindcss from '@tailwindcss/vite' // ⭐️ 1. 테일윈드 도구 가져오기

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(), // ⭐️ 2. 플러그인에 장착!
  ],
})