import axios from 'axios';

export const api = axios.create({ baseURL: '/api/v1' });

api.interceptors.request.use((config) => {
  config.auth = {
    username: localStorage.getItem('user') || '',
    password: localStorage.getItem('password') || '',
  };
  return config;
});

export interface Me {
  username: string;
  authorities: string[];
}

export interface PageResult<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}

export async function fetchMe(): Promise<Me> {
  const res = await api.get<Me>('/me');
  return res.data;
}
