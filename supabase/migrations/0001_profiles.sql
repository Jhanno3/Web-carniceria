-- Tasks T1 y T2 de specs/001-autenticacion-y-perfil/tasks.md
-- Correr en el SQL Editor del proyecto de Supabase (o vía `supabase db push` si se linkea el CLI).
-- No se puede aplicar desde el backend: hace falta la contraseña de la base (Project Settings → Database),
-- que todavía no tenemos en backend/.env.local.

-- ========== T1: tabla profiles + trigger de creación automática ==========

create table if not exists public.profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  role text not null default 'client' check (role in ('client', 'admin')),
  first_name text,
  last_name text,
  phone text,
  address_text text,
  address_lat double precision,
  address_lng double precision,
  created_at timestamptz not null default now()
);

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
  insert into public.profiles (id, role)
  values (new.id, 'client');
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- ========== T2: Row Level Security ==========

alter table public.profiles enable row level security;

-- Cada usuario lee su propia fila.
create policy "profiles_select_own"
  on public.profiles for select
  using (auth.uid() = id);

-- Cada usuario edita su propia fila, pero nunca puede cambiarse el role a sí mismo
-- (ver la capa model.Profile del backend: ahí se refuerza esta misma regla a nivel aplicación).
create policy "profiles_update_own"
  on public.profiles for update
  using (auth.uid() = id)
  with check (auth.uid() = id and role = (select role from public.profiles where id = auth.uid()));

-- El admin lee y edita todas las filas.
create policy "profiles_admin_select_all"
  on public.profiles for select
  using (
    exists (select 1 from public.profiles p where p.id = auth.uid() and p.role = 'admin')
  );

create policy "profiles_admin_update_all"
  on public.profiles for update
  using (
    exists (select 1 from public.profiles p where p.id = auth.uid() and p.role = 'admin')
  );
