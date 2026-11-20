# VB G-RAM-G — Administration Portal

A responsive administrative and financial management portal built with static HTML/CSS/JavaScript and Supabase. GitHub Pages serves the interface; Supabase provides authentication, PostgreSQL data, row-level security and transactional workflows.

## Main modules

- Dashboard: TA, work, billing and outstanding summaries.
- Technical Assistant Directory: profile records, block assignment and Gram Panchayat mapping.
- Work Management: create and edit work records, assign a TA and choose only a Gram Panchayat mapped to that TA.
- Estimate Generator: create project report and forwarding-note drafts with GP names (internal GP codes are not printed).
- Accounts & Payments: record payments through a database RPC, allocate oldest outstanding works first, view the payment ledger and generate summary-only bills addressed to individual Technical Assistants.
- Reports & Records: TA-wise work and financial summaries with CSV export.
- Audit History: append-only activity history for profile, work, payment and allocation changes.

## Existing database and safe upgrade

The original tables and records are retained. Do not drop or recreate the original tables.

1. Keep the existing Supabase project, Auth users, user_roles, and existing data.
2. Open Supabase Dashboard → SQL Editor.
3. Run **supabase-improvements.sql** once. This is an additive migration. It creates payment_allocations and audit_logs, adds audit triggers, protects paid_amount from direct browser edits, restricts direct payment-ledger writes, and installs the atomic record_ta_payment RPC.
4. Refresh the portal after the migration finishes.

The original supabase-schema.sql is for a fresh installation only. Do not rerun it as a replacement for an existing production database.

### Historical payment handling

Existing payment rows and existing work_tracker.paid_amount values are preserved. Older payments are deliberately marked **Legacy / unmapped** in the ledger because the old database did not store a reliable payment-to-work allocation relationship. The migration does not guess or fabricate those links. New payments are allocated transactionally to the oldest outstanding works, and each allocation is recorded.

### Financial workflow

- Work status (Pending, In Progress, Completed) is independent of payment status.
- Payment status is derived from bill_amount and paid_amount; receiving money does not automatically mark a work as completed.
- A payment must be positive, have a valid date not in the future, and not exceed the TA's outstanding amount.
- A payment, its work allocations, and audit events are committed in one database transaction or all rolled back.
- Allocations are oldest-first by received date, then work ID.
- Direct changes to paid_amount are blocked; use Accounts & Payments.
- Profile records with work history are protected from deletion in the interface.

## Security

- Supabase email/password authentication remains in use.
- Access to workspace tables is controlled by the existing user_roles membership and RLS policies.
- Never put a Supabase service_role key in browser code.
- TA plaintext passwords must not be imported or stored. Use Supabase Auth.
- Audit history is readable by authorized workspace users and has no browser write policy.

## Important limitations / operational notes

- Audit history begins when the additive migration is installed; it cannot recreate earlier change history.
- Legacy payments remain unmapped until an administrator reconciles them manually using trustworthy source records.
- Verify estimates and generated statements before official submission.
- The app uses the existing TA names in work and payment records; renaming a TA should be handled carefully to avoid breaking historical name-based associations.
- GitHub Pages deployment and Supabase Authentication URL allow-list must be configured for the deployed site.
