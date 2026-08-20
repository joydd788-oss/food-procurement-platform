import { reactive } from 'vue';
import { fetchMe } from './api';

export const auth = reactive({
  username: '',
  authorities: [] as string[],
  checked: false,
});

export function hasRole(role: string): boolean {
  return auth.authorities.includes('ROLE_' + role);
}

export function isSupplierOnly(): boolean {
  return hasRole('SUPPLIER') && !hasRole('ADMIN') && !hasRole('BUYER') && !hasRole('REGULATOR');
}

export async function initAuth(): Promise<void> {
  if (!localStorage.getItem('user')) {
    auth.checked = true;
    return;
  }
  try {
    const me = await fetchMe();
    auth.username = me.username;
    auth.authorities = me.authorities;
  } catch {
    auth.username = '';
    auth.authorities = [];
  } finally {
    auth.checked = true;
  }
}

export async function login(username: string, password: string): Promise<void> {
  localStorage.setItem('user', username);
  localStorage.setItem('password', password);
  try {
    const me = await fetchMe();
    auth.username = me.username;
    auth.authorities = me.authorities;
    auth.checked = true;
  } catch (e) {
    localStorage.removeItem('user');
    localStorage.removeItem('password');
    throw e;
  }
}

export function logout(): void {
  localStorage.clear();
  auth.username = '';
  auth.authorities = [];
  auth.checked = true;
}
