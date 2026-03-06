import { Link, useNavigate } from 'react-router-dom';

export default function Header() {
    const navigate = useNavigate();
    const token = localStorage.getItem('token'); // 로그인 상태 확인

    const handleLogout = () => {
        localStorage.removeItem('token'); // 금고 비우기
        alert("로그아웃 되었습니다.");
        navigate('/login');
    };

    return (
        <nav className="flex justify-between items-center p-5 bg-white border-b border-gray-200 sticky top-0 z-50">
            <Link to="/" className="text-2xl font-black text-blue-600 tracking-tighter">
                CREPIC
            </Link>

            <div className="flex gap-6 items-center font-medium text-gray-600">
                <Link to="/" className="hover:text-blue-500 transition">탐색</Link>
                <Link to="/" className="hover:text-blue-500 transition">가격</Link>

                {token ? (
                    <button
                        onClick={handleLogout}
                        className="px-4 py-2 bg-gray-100 text-gray-800 rounded-full hover:bg-gray-200 transition"
                    >
                        로그아웃
                    </button>
                ) : (
                    <Link
                        to="/login"
                        className="px-4 py-2 bg-blue-600 text-white rounded-full hover:bg-blue-700 transition"
                    >
                        로그인
                    </Link>
                )}
            </div>
        </nav>
    );
}