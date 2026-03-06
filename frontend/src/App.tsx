import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Main from './pages/Main';
import Login from './pages/Login';

function App() {
    return (
        <BrowserRouter>
            <Routes>
                {/* 주소창에 아무것도 안 치면(/) Main 방을 보여줘! */}
                <Route path="/" element={<Main />} />

                {/* 주소창에 /login을 치면 Login 방을 보여줘! */}
                <Route path="/login" element={<Login />} />
            </Routes>
        </BrowserRouter>
    );
}

export default App;