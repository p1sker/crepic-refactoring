import { atom } from 'jotai';

// ⭐️ S+++급 포인트: 이메일과 토큰 정보를 담는 '원자'를 만듭니다.
// 초기값은 localStorage에서 가져오되, 없으면 null로 설정합니다.
export const tokenAtom = atom<string | null>(localStorage.getItem('token'));
export const userEmailAtom = atom<string | null>(null);

// ⭐️ 파생된 원자 (백엔드의 계산된 필드 느낌): 로그인 여부를 불리언으로 반환
export const isLoggedInAtom = atom((get) => get(tokenAtom) !== null);