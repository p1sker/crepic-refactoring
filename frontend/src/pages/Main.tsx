import Header from '../components/Header';

export default function Main() {
    // 임시 데이터 (나중에 백엔드에서 받아올 사진들)
    const dummyImages = [1, 2, 3, 4, 5, 6, 7, 8];

    return (
        <div className="min-h-screen bg-white">
            <Header />

            {/* 히어로 섹션: 첫 화면 강렬한 문구 */}
            <section className="py-20 px-5 text-center bg-linear-to-b from-blue-50 to-white">
                <h1 className="text-5xl md:text-6xl font-extrabold text-gray-900 mb-6 tracking-tight">
                    세상의 모든 찰나를 <span className="text-blue-600">거래하세요.</span>
                </h1>
                <p className="text-xl text-gray-600 mb-10 max-w-2xl mx-auto">
                    Crepic은 고퀄리티 이미지를 사고파는 가장 진보된 이미지 마켓플레이스입니다.
                </p>
                <div className="flex justify-center gap-4">
                    <button className="px-8 py-4 bg-blue-600 text-white rounded-xl font-bold text-lg shadow-lg shadow-blue-200 hover:scale-105 transition">
                        지금 시작하기
                    </button>
                    <button className="px-8 py-4 bg-white text-gray-700 border border-gray-200 rounded-xl font-bold text-lg hover:bg-gray-50 transition">
                        작품 구경하기
                    </button>
                </div>
            </section>

            {/* 이미지 그리드 섹션 */}
            <main className="max-w-7xl mx-auto px-5 py-16">
                <h2 className="text-2xl font-bold mb-8 text-gray-800">최근 업로드된 이미지</h2>

                {/* S+++급 기술: 테일윈드 그리드 시스템 */}
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
                    {dummyImages.map((id) => (
                        <div key={id} className="group relative overflow-hidden rounded-2xl bg-gray-200 aspect-[4/5] cursor-pointer">
                            {/* 실제 이미지가 없으니 색상 박스로 대체 */}
                            <div className={`w-full h-full bg-blue-${id % 9}00 opacity-20 group-hover:scale-110 transition duration-500`}></div>

                            {/* 이미지 정보 호버 효과 */}
                            <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex flex-col justify-end p-5">
                                <p className="text-white font-bold">Crepic Artwork #{id}</p>
                                <p className="text-gray-200 text-sm">@photographer_p1sker</p>
                            </div>
                        </div>
                    ))}
                </div>
            </main>
        </div>
    );
}