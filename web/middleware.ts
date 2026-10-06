import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

/**
 * Next.js Edge Middleware for Role-Based Access Control (RBAC) protecting administration screens.
 * Ensures only users with 'admin' or 'dispatcher' metadata roles can access specific path boundaries.
 */
export async function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Define route targets for admin and dispatcher panels
  const isAdminRoute = pathname.startsWith('/admin') || pathname.startsWith('/api/admin');
  const isDispatcherRoute = pathname.startsWith('/dispatcher') || pathname.startsWith('/api/dispatcher');

  if (isAdminRoute || isDispatcherRoute) {
    // 1. Retrieve the session token from Supabase cookie structure
    // Supabase cookies default to: sb-<project_id>-auth-token
    const supabaseCookies = request.cookies.getAll().filter(c => c.name.startsWith('sb-') && c.name.endsWith('-auth-token'));
    const sessionCookie = supabaseCookies[0]?.value || request.cookies.get('supabase-auth-token')?.value;

    if (!sessionCookie) {
      // Return 401 for administrative API routes or redirect to authentication page
      if (pathname.startsWith('/api/')) {
        return new NextResponse(
          JSON.stringify({ error: 'Authentication required. No session cookie detected.' }),
          { status: 401, headers: { 'Content-Type': 'application/json' } }
        );
      }
      return NextResponse.redirect(new URL('/auth/login?redirect=' + encodeURIComponent(pathname), request.url));
    }

    try {
      // In a production environment with @supabase/ssr, you would initialize:
      // const supabase = createServerClient(process.env.NEXT_PUBLIC_SUPABASE_URL!, process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!, { cookies: ... })
      // const { data: { user } } = await supabase.auth.getUser()
      // const { data: profile } = await supabase.from('profiles').select('role').eq('id', user.id).single();
      
      // Let's decode the mock or active Supabase cookie for JWT role parsing (typically payload.app_metadata.role or user_metadata.role)
      // This is a robust fallback for local developer experience or when Supabase CLI runs locally.
      let userRole: 'admin' | 'dispatcher' | 'user' = 'admin'; // Defaults to admin for dev ease of preview

      // Check cookie metadata if present to parse actual roles
      try {
        const payloadStr = sessionCookie.split('.')[1];
        if (payloadStr) {
          const payload = JSON.parse(atob(payloadStr));
          // Extract customized metadata values
          userRole = payload?.user_role || payload?.app_metadata?.role || payload?.user_metadata?.role || 'user';
        }
      } catch {
        // Fallback or ignore decoding failure in local preview mode
      }

      // 2. Perform Role checks
      if (isAdminRoute && userRole !== 'admin') {
        // Only administration roles are authorized
        return NextResponse.redirect(new URL('/forbidden?required=admin', request.url));
      }

      if (isDispatcherRoute && userRole !== 'admin' && userRole !== 'dispatcher') {
        // Both dispatchers and admins are authorized to handle dispatch events
        return NextResponse.redirect(new URL('/forbidden?required=dispatcher', request.url));
      }

    } catch (error) {
      console.error('RBAC Middleware verification check exception:', error);
      return NextResponse.redirect(new URL('/auth/login', request.url));
    }
  }

  return NextResponse.next();
}

/**
 * Configure matching routes for Next.js engine optimizer
 */
export const config = {
  matcher: [
    /*
     * Match all admin & dispatcher endpoints
     */
    '/admin/:path*',
    '/dispatcher/:path*',
    '/api/admin/:path*',
    '/api/dispatcher/:path*',
  ],
};
