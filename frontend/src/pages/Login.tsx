import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query'; // 🛡️ 2번 무기: 비동기 관리
import { useSetAtom } from 'jotai';                 // ⚛️ 3번 무기: 전역 상태 관리
import { authApi } from '../api/auth';
import { tokenAtom } from '../store/auth';

export default function Login() {
    // 1. 로컬 상태 (입력값 관리)
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const navigate = useNavigate();

    // 2. Jotai 전역 상태 업데이트 함수 (Setter)
    const setToken = useSetAtom(tokenAtom);

    // 3. TanStack Query Mutation (비동기 로직의 컨트롤러 역할)
    const loginMutation = useMutation({
        mutationFn: authApi.login, // 우리 리포지토리(api/auth.ts)의 함수 실행
        onSuccess: (data) => {
            // 성공 시 로직: 토큰 저장 및 페이지 이동
            localStorage.setItem('token', data.accessToken);
            setToken(data.accessToken); // ⚛️ Jotai 원자(Atom) 업데이트 -> 헤더 등 실시간 반영

            setTimeout(() => {
                navigate('/');
            }, 1000);
        },
        // 에러 발생 시 별도의 처리 없이 UI에서 loginMutation.isError로 감지 가능
    });

    // 4. 이벤트 핸들러
    const handleLogin = (e: React.FormEvent) => {
        e.preventDefault();
        // mutate 함수를 호출하면 위에서 정의한 mutationFn이 실행됩니다.
        loginMutation.mutate({ email, password });
    };

    return (
        <div className="flex justify-center items-center min-h-screen bg-gray-100">
            <div className="w-full max-w-md p-8 bg-white rounded-xl shadow-lg border border-gray-200">
                <h2 className="text-4xl font-black text-center text-blue-600 mb-8 tracking-tighter">CREPIC</h2>

                <form onSubmit={handleLogin} className="flex flex-col gap-5">
                    <div className="flex flex-col gap-1">
                        <label className="text-sm font-semibold text-gray-600 ml-1">이메일</label>
                        <input
                            type="email"
                            placeholder="example@crepic.com"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            className="p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none transition"
                            required
                        />
                    </div>

                    <div className="flex flex-col gap-1">
                        <label className="text-sm font-semibold text-gray-600 ml-1">비밀번호</label>
                        <input
                            type="password"
                            placeholder="••••••••"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            className="p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none transition"
                            required
                        />
                    </div>

                    <button
                        type="submit"
                        disabled={loginMutation.isPending} // 로딩 중 버튼 중복 클릭 방지
                        className={`p-4 mt-2 text-white rounded-xl font-bold text-lg shadow-md transition duration-200 ${
                            loginMutation.isPending
                                ? 'bg-gray-400 cursor-not-allowed'
                                : 'bg-blue-600 hover:bg-blue-700 active:scale-95'
                        }`}
                    >
                        {loginMutation.isPending ? '로그인 중...' : '로그인'}
                    </button>
                </form>

                {/* 선언적 UI 처리: Mutation 상태에 따라 메시지 출력 */}
                <div className="h-6 mt-6 text-center">
                    {loginMutation.isError && (
                        <p className="text-sm font-bold text-red-500 animate-bounce">
                            ❌ 로그인 실패: 이메일이나 비밀번호를 확인하세요.
                        </p>
                    )}
                    {loginMutation.isSuccess && (
                        <p className="text-sm font-bold text-green-600">
                            ✅ 로그인 성공! 메인으로 이동합니다.
                        </p>
                    )}
                </div>
            </div>
        </div>
    );
}