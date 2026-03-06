import axios from 'axios';
import type {LoginRequest, LoginResponse} from "../types/auth.ts";


// 공통 설정을 가진 axios 인스턴스 (Spring의 RestTemplate 설정과 비슷함)
const api = axios.create({
    baseURL: 'http://localhost:8080/api',
});

export const authApi = {
    // 로그인 요청 함수
    login: async (data: LoginRequest): Promise<LoginResponse> => {
        const response = await api.post<LoginResponse>('/members/login', data);
        return response.data;
    },
};