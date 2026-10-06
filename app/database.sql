
-- Type Definitions
DO $$ BEGIN
    CREATE TYPE public.user_role AS ENUM ('user', 'admin', 'guardian');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE public.sos_status AS ENUM ('active', 'resolved', 'false_alarm');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Profile table
create table if not exists public.profiles (
  id uuid not null references auth.users(id) on delete cascade,
  phone_number text null,
  full_name text not null,
  avatar_url text null,
  is_premium boolean not null default false,
  trip_credits integer not null default 0,
  points_balance integer not null default 0,
  role public.user_role not null default 'user'::user_role,
  dob text null,
  gender text null,
  nid_passport text null,
  occupation text null,
  present_address text null,
  permanent_address text null,
  blood_group text null,
  allergies text null,
  chronic_conditions text null,
  current_medications text null,
  height_weight text null,
  special_needs text null,
  constraint profiles_pkey primary key (id)
) TABLESPACE pg_default;

-- Travel Activity (Trips)
create table if not exists public.travel_activities (
  id uuid not null default gen_random_uuid (),
  user_id uuid null references auth.users(id) on delete cascade,
  transport_mode text not null,
  start_address text null,
  end_address text null,
  start_coords text null,
  end_coords text null,
  start_time timestamp with time zone null default now(),
  end_time timestamp with time zone null,
  total_distance numeric null,
  vehicle_plate_number text null,
  vehicle_description text null,
  vehicle_photo_url text null,
  driver_name_manual text null,
  safety_status text null default 'ongoing'::text,
  sos_triggered_at timestamp with time zone null,
  route_path_log jsonb null,
  route_deviation_count integer null default 0,
  unexpected_stop_logs jsonb null,
  speed_profile jsonb null,
  start_battery_level integer null,
  end_battery_level integer null,
  is_gps_lost boolean null default false,
  device_model text null,
  notified_guardians text[] null,
  guardian_check_in_count integer null default 0,
  audio_clip_url text null,
  impact_detected boolean null default false,
  area_safety_score numeric null,
  tracking_code text null,
  created_at timestamp with time zone null default now(),
  constraint travel_activities_pkey primary key (id),
  constraint travel_activities_tracking_code_key unique (tracking_code)
) TABLESPACE pg_default;

-- Trip Shares table (New for Guardian Watch)
create table if not exists public.trip_shares (
  id uuid not null default gen_random_uuid (),
  trip_id uuid not null references public.travel_activities(id) on delete cascade,
  shared_by uuid not null references public.profiles(id) on delete cascade,
  shared_with_phone text not null,
  shared_with_user_id uuid null references public.profiles(id) on delete set null,
  status text default 'active'::text,
  created_at timestamp with time zone default now(),
  constraint trip_shares_pkey primary key (id)
) TABLESPACE pg_default;

-- SOS Record
create table if not exists public.sos_records (
  id uuid not null default gen_random_uuid (),
  trip_id uuid not null references public.travel_activities(id) on delete cascade,
  triggered_at timestamp with time zone not null default now(),
  status public.sos_status not null default 'active'::sos_status,
  constraint sos_records_pkey primary key (id)
) TABLESPACE pg_default;

-- Safety News
create table if not exists public.safety_news (
  id uuid not null default extensions.uuid_generate_v4 (),
  title text not null,
  description text not null,
  content text null,
  image_url text null,
  category text null default 'General'::text,
  is_published boolean null default true,
  published_at timestamp with time zone null default now(),
  created_at timestamp with time zone null default now(),
  constraint safety_news_pkey primary key (id)
) TABLESPACE pg_default;

-- Banners Table
create table if not exists public.banners (
  id uuid not null default extensions.uuid_generate_v4 (),
  title text not null,
  image_url text not null,
  action_url text null,
  display_order integer null default 0,
  is_active boolean null default true,
  created_at timestamp with time zone null default now(),
  constraint banners_pkey primary key (id)
) TABLESPACE pg_default;

-- Guardian tables
create table if not exists public.guardians (
  id uuid not null default gen_random_uuid (),
  user_id uuid null references auth.users(id) on delete cascade,
  name text not null,
  relationship text null,
  phone text not null,
  avatar_index integer null default 0,
  default_notify boolean null default true,
  sos_permission boolean null default true,
  created_at timestamp with time zone null default now(),
  constraint guardians_pkey primary key (id)
) TABLESPACE pg_default;

-- User Devices table (For storing FCM Push Notification Tokens)
create table if not exists public.user_devices (
  id uuid not null default gen_random_uuid (),
  user_id uuid not null references public.profiles(id) on delete cascade,
  fcm_token text not null,
  created_at timestamp with time zone default now(),
  updated_at timestamp with time zone default now(),
  constraint user_devices_pkey primary key (id),
  constraint user_devices_fcm_token_key unique (fcm_token)
) TABLESPACE pg_default;

ALTER TABLE public.user_devices ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can manage their own devices" ON public.user_devices;
CREATE POLICY "Users can manage their own devices" ON public.user_devices
  FOR ALL
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can view devices of shared trips" ON public.user_devices;
CREATE POLICY "Users can view devices of shared trips" ON public.user_devices
  FOR SELECT
  USING (true);

-- Points Log table
create table if not exists public.points_log (
  id uuid not null default gen_random_uuid (),
  user_id uuid not null references public.profiles(id) on delete cascade,
  points_added integer not null,
  source text not null,
  timestamp timestamp with time zone not null default now(),
  constraint points_log_pkey primary key (id)
) TABLESPACE pg_default;

-- User Feedback table
create table if not exists public.feedback (
  id uuid not null default gen_random_uuid (),
  user_id uuid null references auth.users(id) on delete cascade,
  rating integer not null check (rating >= 1 and rating <= 5),
  feedback_type text not null,
  subject text null,
  message text not null,
  created_at timestamp with time zone null default now(),
  constraint feedback_pkey primary key (id)
) TABLESPACE pg_default;

-- ==========================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ==========================================

-- Enable RLS on all tables
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.travel_activities ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.sos_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.guardians ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.points_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.feedback ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.trip_shares ENABLE ROW LEVEL SECURITY;
-- Public tables (read-only for users)
ALTER TABLE public.safety_news ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.banners ENABLE ROW LEVEL SECURITY;

-- 1. Profiles Policies
-- ==============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ==============================================================================

-- 1. Drop all legacy recursive helper functions to eliminate any chance of recursion
DROP FUNCTION IF EXISTS public.get_current_user_phone() CASCADE;
DROP FUNCTION IF EXISTS public.get_user_role(uuid) CASCADE;
DROP FUNCTION IF EXISTS public.is_admin() CASCADE;

-- Enable RLS on all tables
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.travel_activities ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.sos_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.guardians ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.points_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.feedback ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.trip_shares ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.safety_news ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.banners ENABLE ROW LEVEL SECURITY;

-- 1. Profiles Table Policies (Fresh Clean Setup)
-- ==============================================================================

-- Grant explicit table permissions to API roles
GRANT ALL ON TABLE public.profiles TO anon, authenticated, service_role;

-- Dynamically drop ALL existing policies on public.profiles
DO $$
DECLARE
    pol RECORD;
BEGIN
    FOR pol IN 
        SELECT policyname 
        FROM pg_policies 
        WHERE tablename = 'profiles' AND schemaname = 'public'
    LOOP
        EXECUTE format('DROP POLICY IF EXISTS %I ON public.profiles', pol.policyname);
    END LOOP;
END $$;

-- Reset RLS on profiles table
ALTER TABLE public.profiles DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- Create fresh, simple RLS policies for profiles
CREATE POLICY "profiles_select_policy"
ON public.profiles FOR SELECT
TO public
USING ( true );

CREATE POLICY "profiles_insert_policy"
ON public.profiles FOR INSERT
TO public
WITH CHECK ( true );

CREATE POLICY "profiles_update_policy"
ON public.profiles FOR UPDATE
TO public
USING ( true )
WITH CHECK ( true );

CREATE POLICY "profiles_delete_policy"
ON public.profiles FOR DELETE
TO public
USING ( true );

-- ------------------------------------------------------------------------------
-- 2. Travel Activities (Trips) Policies
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "Users can view own trips" ON public.travel_activities;
DROP POLICY IF EXISTS "Guardians can view shared travel activities" ON public.travel_activities;
DROP POLICY IF EXISTS "Users can insert own trips" ON public.travel_activities;
DROP POLICY IF EXISTS "Users can update own trips" ON public.travel_activities;

CREATE POLICY "Users can view own trips"
ON public.travel_activities FOR SELECT
USING ( auth.uid() = user_id OR tracking_code IS NOT NULL );

CREATE POLICY "Users can insert own trips"
ON public.travel_activities FOR INSERT
WITH CHECK ( auth.uid() = user_id OR user_id IS NULL );

CREATE POLICY "Users can update own trips"
ON public.travel_activities FOR UPDATE
USING ( auth.uid() = user_id );

-- ------------------------------------------------------------------------------
-- 3. Guardians Policies
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "Users can manage own guardians" ON public.guardians;
CREATE POLICY "Users can manage own guardians"
ON public.guardians FOR ALL
USING ( auth.uid() = user_id )
WITH CHECK ( auth.uid() = user_id );

-- ------------------------------------------------------------------------------
-- 4. Points Log Policies
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "Users can view own points" ON public.points_log;
DROP POLICY IF EXISTS "Users can insert own points" ON public.points_log;

CREATE POLICY "Users can view own points"
ON public.points_log FOR SELECT
USING ( auth.uid() = user_id );

CREATE POLICY "Users can insert own points"
ON public.points_log FOR INSERT
WITH CHECK ( auth.uid() = user_id );

-- ------------------------------------------------------------------------------
-- 5. Feedback Policies
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "Users can insert own feedback" ON public.feedback;
DROP POLICY IF EXISTS "Users can view own feedback" ON public.feedback;

CREATE POLICY "Users can insert own feedback"
ON public.feedback FOR INSERT
WITH CHECK ( auth.uid() = user_id OR user_id IS NULL );

CREATE POLICY "Users can view own feedback"
ON public.feedback FOR SELECT
USING ( auth.uid() = user_id );

-- ------------------------------------------------------------------------------
-- 6. Trip Shares Policies
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "Users can view trips they shared" ON public.trip_shares;
DROP POLICY IF EXISTS "Guardians can view trips shared with them" ON public.trip_shares;
DROP POLICY IF EXISTS "Users can create their own trip shares" ON public.trip_shares;

CREATE POLICY "Users can view trips they shared"
ON public.trip_shares FOR SELECT
USING ( auth.uid() = shared_by OR auth.uid() = shared_with_user_id OR true );

CREATE POLICY "Users can create their own trip shares"
ON public.trip_shares FOR INSERT
WITH CHECK ( auth.uid() = shared_by );

-- ------------------------------------------------------------------------------
-- 7. SOS Records Policies
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "Users can view and insert SOS records" ON public.sos_records;
CREATE POLICY "Users can view and insert SOS records"
ON public.sos_records FOR ALL
USING ( true )
WITH CHECK ( true );

-- ------------------------------------------------------------------------------
-- 8. Public Content (Safety News & Banners)
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "Anyone can view news" ON public.safety_news;
CREATE POLICY "Anyone can view news"
ON public.safety_news FOR SELECT
USING ( true );

DROP POLICY IF EXISTS "Anyone can view banners" ON public.banners;
CREATE POLICY "Anyone can view banners"
ON public.banners FOR SELECT
USING ( true );

-- 7. Police Station
create table if not exists public.police_stations (
  id serial not null,
  policestation_name character varying(255) null,
  division character varying(100) null,
  district character varying(100) null,
  thana character varying(100) null,
  type character varying(50) null,
  policestation_number character varying(20) null,
  latitude numeric(10, 8) null,
  longitude numeric(11, 8) null,
  address text null,
  policestation_image text null,
  constraint police_stations_pkey1 primary key (id)
) TABLESPACE pg_default;

-- 8. Fire Service Station
create table if not exists public.fire_stations (
  id serial not null,
  firestation_name character varying(255) null,
  division character varying(100) null,
  district character varying(100) null,
  thana character varying(100) null,
  type character varying(50) null,
  firestation_number character varying(20) null,
  latitude numeric(10, 8) null,
  longitude numeric(11, 8) null,
  address text null,
  firestation_image text null,
  website character varying(255) null,
  maps_category character varying(100) null,
  google_maps_url text null,
  source_query text null,
  constraint fire_stations_pkey primary key (id),
  constraint fire_stations_firestation_name_key unique (firestation_name),
  constraint unique_fireestation_name unique (firestation_name),
  constraint unique_firestation_name unique (firestation_name)
) TABLESPACE pg_default;

-- RLS for Stations
ALTER TABLE public.police_stations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.fire_stations ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Anyone can view police stations" ON public.police_stations;
CREATE POLICY "Anyone can view police stations" ON public.police_stations FOR SELECT USING (true);

DROP POLICY IF EXISTS "Anyone can view fire stations" ON public.fire_stations;
CREATE POLICY "Anyone can view fire stations" ON public.fire_stations FOR SELECT USING (true);

DROP POLICY IF EXISTS "Admins can manage police stations" ON public.police_stations;
CREATE POLICY "Admins can manage police stations" ON public.police_stations FOR ALL USING (is_admin());

DROP POLICY IF EXISTS "Admins can manage fire stations" ON public.fire_stations;
CREATE POLICY "Admins can manage fire stations" ON public.fire_stations FOR ALL USING (is_admin());

-- ==========================================
-- SAFETY TIPS MODULE
-- ==========================================

-- Safety Tips table
create table if not exists public.safety_tips (
  id uuid not null default extensions.uuid_generate_v4 (),
  title text not null,
  description text not null,
  content text not null,
  category text not null,
  image_url text null,
  is_published boolean null default true,
  created_at timestamp with time zone null default now(),
  constraint safety_tips_pkey primary key (id)
) TABLESPACE pg_default;

-- User Tip Progress (to track read status)
create table if not exists public.user_tip_progress (
  id uuid not null default gen_random_uuid (),
  user_id uuid not null references auth.users(id) on delete cascade,
  tip_id uuid not null references public.safety_tips(id) on delete cascade,
  is_read boolean not null default true,
  read_at timestamp with time zone not null default now(),
  constraint user_tip_progress_pkey primary key (id),
  constraint unique_user_tip unique (user_id, tip_id)
) TABLESPACE pg_default;

-- RLS for Safety Tips
ALTER TABLE public.safety_tips ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_tip_progress ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can view safety tips" ON public.safety_tips FOR SELECT USING (true);
CREATE POLICY "Users can manage own tip progress" ON public.user_tip_progress FOR ALL USING (auth.uid() = user_id);

-- Initial Safety Tips Data
INSERT INTO public.safety_tips (title, description, content, category) VALUES
('ডকুমেন্ট যাচাই', 'আপনার প্রয়োজনীয় ডকুমেন্টের ডিজিটাল কপি ফোনে সেভ রাখুন।', 'আপনার NID, পাসপোর্ট বা প্রয়োজনীয় ডকুমেন্টের একটি করে ডিজিটাল কপি ফোনে সেভ রাখুন। ভ্রমণের সময় হারানো বা চুরির ক্ষেত্রে এটি খুবই কাজে লাগে।', 'Pre-Journey'),
('জরুরি কন্টাক্ট', 'বিশ্বস্ত মানুষকে গন্তব্য এবং ফেরার সময় জানিয়ে রাখুন।', 'আপনার গন্তব্য এবং প্রত্যাশিত ফেরার সময় অন্তত একজন বিশ্বস্ত মানুষকে জানিয়ে রাখুন। আপনার অবস্থান সম্পর্কে তারা নিশ্চিত হতে পারবে।', 'Pre-Journey'),
('ডিভাইস চার্জ', 'ফোনের ব্যাটারি অন্তত ৮০% চার্জ নিশ্চিত করুন।', 'ফোনের ব্যাটারি অন্তত ৮০% চার্জ আছে কি না নিশ্চিত করুন এবং একটি পাওয়ার ব্যাংক সাথে রাখুন। জরুরি যোগাযোগের জন্য ফোন সচল রাখা আবশ্যিক।', 'Pre-Journey'),
('লোকেশন শেয়ারিং', 'প্রিয়জনের সাথে লাইভ লোকেশন শেয়ার অন করুন।', 'গুগল ম্যাপ বা আমাদের অ্যাপের মাধ্যমে আপনার প্রিয়জনের সাথে লাইভ লোকেশন শেয়ার অন করুন। এটি আপনার নিরাপত্তা বহুগুণ বাড়িয়ে দেয়।', 'Pre-Journey'),
('অপরিচিত খাবার', 'অপরিচিত কারো দেওয়া খাবার বা পানীয় গ্রহণ করবেন না।', 'বাসে বা ট্রেনে অপরিচিত কারো দেওয়া পানি, পানীয় বা খাবার গ্রহণ করা থেকে বিরত থাকুন। চেতনানাশক ড্রাগ মিশ্রিত খাবারের ঝুঁকি থাকে।', 'Transport'),
('ব্যাগ নিরাপত্তা', 'ভিড়ের মধ্যে ব্যাগ সবসময় সামনের দিকে রাখুন।', 'ভিড়ের মধ্যে ব্যাগ সবসময় সামনের দিকে ঝুলিয়ে রাখুন। দামী জিনিস পকেটে না রেখে ব্যাগের চেইন দেওয়া পকেটে রাখুন।', 'Transport'),
('যানবাহন যাচাই', 'গাড়ির নাম্বার প্লেট এবং চালকের চেহারা মিলিয়ে নিন।', 'শেয়ারড রাইড বা ট্যাক্সিতে ওঠার আগে গাড়ির নাম্বার প্লেট এবং চালকের চেহারা মিলিয়ে নিন। কোনো গরমিল থাকলে যাত্রা বর্জন করুন।', 'Transport'),
('জরুরি প্রস্থান', 'যানবাহনের জরুরি নির্গমন পথটি দেখে নিন।', 'বাসে বা লঞ্চে ওঠার পর জরুরি নির্গমন পথটি (Emergency Exit) কোথায় তা দেখে নিন। বিপদের সময় দ্রুত বের হওয়ার পরিকল্পনা রাখুন।', 'Transport'),
('উজ্জ্বল ও জনাকীর্ণ স্থান', 'রাতে অন্ধকার গলি এড়িয়ে মেইন রোড দিয়ে চলুন।', 'রাতে একা হাঁটার সময় অন্ধকার গলি এড়িয়ে মেইন রোড বা উজ্জ্বল আলো আছে এমন জায়গা দিয়ে চলুন। এতে ছিনতাই বা হামলার ঝুঁকি কমে।', 'Night & Solo'),
('হেডফোন এড়িয়ে চলা', 'বাইরে হাঁটার সময় কানে হেডফোন দেবেন না।', 'বাইরে হাঁটার সময় কানে হেডফোন দিয়ে ফুল ভলিউমে গান শুনবেন না, এতে আশেপাশের শব্দ বা বিপদ টের পাওয়া যায় না।', 'Night & Solo'),
('টাকার ভাগ', 'সব টাকা এক জায়গায় না রেখে ভাগ করে রাখুন।', 'সব টাকা এক জায়গায় না রেখে ভিন্ন ভিন্ন পকেটে বা ব্যাগে ভাগ করে রাখুন। এক জায়গা থেকে হারিয়ে গেলেও বাকিটা আপনার ব্যাকআপ হিসেবে থাকবে।', 'Night & Solo'),
('প্যানিক বাটন', 'বিপদে পড়লে SOS বাটন ব্যবহার করুন।', 'বিপদে পড়লে অ্যাপের SOS বাটন বা প্যানিক অ্যালার্ম ব্যবহার করতে দ্বিধা করবেন না। আমাদের সিস্টেম দ্রুত রেসপন্স টিমের কাছে সিগন্যাল পাঠাবে।', 'Night & Solo'),
('বৃষ্টিতে সতর্কতা', 'পিচ্ছিল রাস্তায় দৌড়াবেন না।', 'বৃষ্টির সময় পিচ্ছিল রাস্তায় দৌড়াবেন না। বিশেষ করে ওভারব্রিজ বা টাইলস করা মেঝেতে সতর্ক থাকুন।', 'Weather'),
('বিদ্যুৎ থেকে সাবধান', 'ঝড়ের সময় খোলা তার বা খুঁটি থেকে দূরে থাকুন।', 'ঝড়ের সময় খোলা তার বা বৈদ্যুতিক খুঁটি থেকে নিরাপদ দূরত্ব বজায় রাখুন। পানির মাধ্যমে বিদ্যুৎ সঞ্চালিত হতে পারে।', 'Weather');

-- ==========================================
-- HOSPITALS MODULE
-- ==========================================

create table if not exists public.hospitals (
  id serial not null,
  hospital_name character varying(255) not null,
  type character varying(50) null, -- Govt, Private, etc.
  specialty text null, -- General, Cardiac, Eye, etc.
  division character varying(100) null,
  district character varying(100) null,
  thana character varying(100) null,
  address text null,
  phone_number character varying(20) null,
  ambulance_number character varying(20) null,
  latitude numeric(10, 8) null,
  longitude numeric(11, 8) null,
  has_blood_bank boolean default false,
  is_24_7 boolean default true,
  image_url text null,
  services jsonb null, -- e.g. ["ICU", "Oxygen", "X-Ray"]
  created_at timestamp with time zone default now(),
  constraint hospitals_pkey primary key (id)
) TABLESPACE pg_default;

ALTER TABLE public.hospitals ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Anyone can view hospitals" ON public.hospitals;
CREATE POLICY "Anyone can view hospitals" ON public.hospitals FOR SELECT USING (true);

-- Initial Hospital Data (Sample)
INSERT INTO public.hospitals (hospital_name, type, specialty, thana, district, address, phone_number, ambulance_number, latitude, longitude, has_blood_bank, is_24_7, services) VALUES
('ঢাকা মেডিকেল কলেজ হাসপাতাল', 'Govt', 'General', 'Shahbag', 'Dhaka', 'Secretariat Road, Dhaka 1000', '02-55165088', '01711-403870', 23.7275, 90.3980, true, true, '["ICU", "Oxygen", "Trauma Center", "Blood Bank"]'),
('বঙ্গবন্ধু শেখ মুজিব মেডিকেল বিশ্ববিদ্যালয় (BSMMU)', 'Govt', 'Specialized', 'Shahbag', 'Dhaka', 'Shahbag, Dhaka 1000', '02-9661068', '02-9661068', 23.7378, 90.3950, true, true, '["ICU", "CCU", "Diagnostic", "Surgery"]'),
('স্কয়ার হাসপাতাল লিমিটেড', 'Private', 'General', 'Panthapath', 'Dhaka', '18/F Bir Uttam Qazi Nuruzzaman Sarak, Dhaka 1205', '02-8144400', '01713-332448', 23.7512, 90.3850, true, true, '["ICU", "Cardiac", "Emergency", "Luxury Cabin"]'),
('এভারকেয়ার হাসপাতাল ঢাকা', 'Private', 'Specialized', 'Bashundhara', 'Dhaka', 'Plot 81, Block E, Bashundhara R/A, Dhaka 1229', '02-8431661', '01714-090000', 23.8090, 90.4300, true, true, '["World-class ICU", "Pediatrics", "Emergency"]'),
('আনোয়ার খান মডার্ন মেডিকেল কলেজ হাসপাতাল', 'Private', 'General', 'Dhanmondi', 'Dhaka', 'House 17, Road 8, Dhanmondi, Dhaka 1205', '02-9670295', '01711-562916', 23.7430, 90.3830, true, true, '["Emergency", "Blood Bank", "OPD"]');

-- ==========================================
-- BLOOD BANKS MODULE
-- ==========================================

create table if not exists public.blood_banks (
  id serial not null,
  bank_name character varying(255) not null,
  type character varying(50) null, -- Govt, NGO, Private
  division character varying(100) null,
  district character varying(100) null,
  thana character varying(100) null,
  address text null,
  phone_number character varying(20) null,
  emergency_phone character varying(20) null,
  latitude numeric(10, 8) null,
  longitude numeric(11, 8) null,
  image_url text null,
  created_at timestamp with time zone default now(),
  constraint blood_banks_pkey primary key (id)
) TABLESPACE pg_default;

ALTER TABLE public.blood_banks ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Anyone can view blood banks" ON public.blood_banks;
CREATE POLICY "Anyone can view blood banks" ON public.blood_banks FOR SELECT USING (true);

-- Initial Blood Bank Data (Sample)
INSERT INTO public.blood_banks (bank_name, type, thana, district, address, phone_number, emergency_phone, latitude, longitude) VALUES
('বাংলাদেশ রেড ক্রিসেন্ট সোসাইটি ব্লাড ব্যাংক', 'NGO', 'Mohammadpur', 'Dhaka', '7/8, Aurongzeb Road, Mohammadpur, Dhaka', '02-9116563', '01811-458521', 23.7660, 90.3620),
('কোয়ান্টাম ফাউন্ডেশন ব্লাড সেন্টার', 'NGO', 'Shanti Nagar', 'Dhaka', '31/V, Shilpacharya Zainul Abedin Sarak, Shanti Nagar, Dhaka', '02-9351969', '01714-047870', 23.7380, 90.4130),
('সন্ধানী ব্লাড ব্যাংক (DMCH Unit)', 'NGO', 'Shahbag', 'Dhaka', 'Dhaka Medical College, Dhaka', '02-9668690', '01552-302325', 23.7275, 90.3980),
('পুলিশ ব্লাড ব্যাংক', 'Govt', 'Malibagh', 'Dhaka', 'Police Lines, Malibagh, Dhaka', '02-9330869', '01713-398487', 23.7480, 90.4110),
('বাধঁন ব্লাড ব্যাংক (TSC)', 'NGO', 'Shahbag', 'Dhaka', 'TSC, University of Dhaka, Dhaka', '02-8629042', '01534-510825', 23.7330, 90.3960);

-- ==========================================
-- EMERGENCY HOTLINES MODULE
-- ==========================================

create table if not exists public.emergency_hotlines (
  id serial not null,
  name character varying(255) not null,
  description text null,
  phone character varying(20) not null,
  category character varying(50) null, -- e.g. National, Women, Children, Legal
  image_url text null,
  created_at timestamp with time zone default now(),
  constraint emergency_hotlines_pkey primary key (id)
) TABLESPACE pg_default;

ALTER TABLE public.emergency_hotlines ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Anyone can view emergency hotlines" ON public.emergency_hotlines;
CREATE POLICY "Anyone can view emergency hotlines" ON public.emergency_hotlines FOR SELECT USING (true);
