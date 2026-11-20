# Secure Suite V3 — GitHub Pages + Supabase

This is a static-web conversion starter for the supplied PHP/MySQL Secure Suite V3 project. GitHub Pages cannot execute PHP, so the UI and data operations have been rewritten to browser JavaScript and Supabase.

## Included in this conversion

- Supabase email/password authentication
- Dashboard totals and recent work register
- TA profile directory: create, edit, search and delete
- Work tracker: create entries, status, TA/block filtering and search
- Estimate number generation (`Est-00001` format)
- Accounts & Payments: payment ledger, oldest-outstanding-first allocation, and individual Technical Assistant bill generation with print / PDF
- VB G-RAM-G report-body and forwarding-note generator with a Gram Panchayat dropdown
- Responsive layout for desktop, tablet and mobile
- Supabase SQL schema with Row Level Security and a private estimates bucket

## 1. Create Supabase project

1. Create a project at https://supabase.com/
2. Open **SQL Editor** and run `supabase-schema.sql`.
3. Open **Project Settings → API**. Copy the Project URL and `anon` / publishable key.
4. Open `app.js` and replace `YOUR_SUPABASE_PROJECT_URL` and `YOUR_SUPABASE_ANON_PUBLIC_KEY`.
5. In **Authentication → Users**, create your first user.
6. Copy that user's UUID and run this SQL in the SQL Editor:

```sql
insert into public.user_roles(user_id, role)
values ('PASTE-AUTH-USER-UUID-HERE', 'admin');
```

Only the public/anon key belongs in the frontend. Never expose a `service_role` key.

## 2. Publish on GitHub Pages

1. Create a GitHub repository, for example `secure-suite-web`.
2. Upload `index.html`, `styles.css`, `app.js`, `supabase-schema.sql`, and `README.md` to the repository root.
3. Open **Settings → Pages**.
4. Under Build and deployment, choose **Deploy from a branch**.
5. Select `main` and `/ (root)`, then Save.
6. Wait for the Pages URL to appear.

## 3. Configure Supabase Auth URL settings

In Supabase **Authentication → URL Configuration**:
- Set Site URL to your GitHub Pages URL.
- Add the same URL under Redirect URLs if prompted.

## Important migration notes

The original PHP project includes server-side features that require further migration before this is a full feature-parity release:

- Existing MySQL data must be exported and imported into the matching Supabase tables. This starter does not automatically migrate records.
- Original PDF upload/replace/download handling needs a Storage UI workflow. The private `estimates` bucket and RLS policy are created, but PDF upload controls and signed-download links are not yet wired into the interface.
- The original payment posting used a database transaction. This starter allocates payment across oldest outstanding jobs from the browser. For production, move payment creation and allocation into a PostgreSQL RPC function so the operation is atomic.
- Do not migrate legacy plaintext profile passwords from `clients.pass`. Use Supabase Auth accounts instead; the new schema does not include a plaintext password field.
- Role granularity, audit history, CSV/Excel exports, printable invoice/statement parity and bulk village import need to be added after the first deployment.
- Review the source app's estimate templates and test every work category before official use. Generated text should be checked by the responsible officer.

## Current status

This is a working foundation, not a claim that every PHP feature has already been reproduced. It is intentionally structured so the remaining migration tasks can be added without exposing database credentials.

### Gram Panchayat mapping display correction

The Profile Directory parses legacy mapping lines such as `UP145367 - AALAPUR` into separate Gram Panchayat Code and Gram Panchayat Name columns. Existing Supabase records do not need to be re-imported.

## Technical Assistant bill generation inside Accounts & Payments

Generate an individual statement for the selected Technical Assistant from **Accounts & Payments**. Choose the current financial year or a custom date range. The bill includes the TA's work records, the Gram Panchayat names attached to those works, payment entries, totals and outstanding balance. It can be printed or saved as PDF. Internal Gram Panchayat IDs are not displayed.

### Bill and estimate workflow

- Bill generation is part of **Accounts & Payments**, not a separate menu.
- Bills are made out to an individual **Technical Assistant**, not to a Gram Panchayat.
- Select a Gram Panchayat by name in **Estimate Generator**; internal mapping codes are not displayed in generated estimates.
