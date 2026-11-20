# Secure Suite V3 — GitHub Pages + Supabase

Static-web conversion of the supplied PHP/MySQL project. GitHub Pages cannot execute PHP; the interface and data operations use browser JavaScript and Supabase.

## Included
- Supabase email/password authentication
- Dashboard, TA Profile Directory and Work Tracker
- Accounts & Payments with payment ledger and an individual Technical Assistant bill generator (print / Save as PDF)
- VB G-RAM-G Estimate Generator with a Gram Panchayat name dropdown
- Responsive layout and Supabase schema with Row Level Security

## Setup
1. Run `supabase-schema.sql` in the Supabase SQL Editor.
2. Configure the Project URL and public anon/publishable key in `app.js`.
3. Create the first user in Supabase Authentication, then assign the admin role using the UUID insert statement at the bottom of the SQL schema.
4. Publish the repository root through GitHub Pages.
5. Add the GitHub Pages URL to Supabase Authentication URL Configuration.

Never put a Supabase `service_role` key in frontend code. Legacy plaintext passwords must not be migrated; use Supabase Auth.

## Data and display rules
- Profile Directory parses mappings such as `UP145367 - AALAPUR` into separate code and name fields.
- The GP internal code is not displayed in estimate or bill output; only the GP name is used.
- Bills are generated inside Accounts & Payments and addressed to an individual Technical Assistant.
- Estimate Generator includes a Gram Panchayat dropdown populated from TA profile mappings.

## Limitations
MySQL records require a separate import. PDF upload/download controls, atomic payment allocation via a database RPC, audit history, exports, and full PHP feature parity are not yet implemented. Verify all generated statements and estimate text before official use.
