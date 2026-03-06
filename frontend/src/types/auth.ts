// 백엔드로 보낼 로그인 요청 데이터 (LoginRequest)
export interface LoginRequest {
    email: string;
    password: string;
}

// 백엔드에서 받을 성공 응답 데이터 (LoginResponse)
export interface LoginResponse {
    accessToken: string;
    message: string;
}