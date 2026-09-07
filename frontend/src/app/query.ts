import { QueryClient } from '@tanstack/react-query';
import type { Profile } from '../api/types';
import { ApiError } from '../api/client';

export const queryClient = new QueryClient({ defaultOptions: { queries: {
  staleTime: 15_000,
  retry: (count, error) => !(error instanceof ApiError && error.status < 500) && count < 1,
} } });
export const accountKey = ['account'] as const;
export const profileKey = (profile: Profile, resource: string) => ['profile', profile.role, profile.id, resource] as const;
// RH&P-025 mutations should invalidate this prefix after a server-confirmed result.
export const refreshProfileData = () => queryClient.invalidateQueries({ queryKey: ['profile'] });
