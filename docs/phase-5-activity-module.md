# JoinUp Phase 5: joinup-activity Module

## Goal
Implement activity creation, update, detail query, page query, cancel, and admin review skeleton.

## Module Placement
- `joinup-activity`
  - `dto`
  - `vo`
  - `entity`
  - `mapper`
  - `service`
  - `controller`
  - `domain`
- `joinup-infra`
  - `joinup_schema_v1.sql` activity table field reservation update
- `joinup-common`
  - activity-specific error codes

## Status Design
Status enum:
- `DRAFT`
- `PENDING_REVIEW`
- `SIGNUP_OPEN`
- `FULL`
- `WAITLIST_OPEN`
- `GROUP_SUCCESS`
- `GROUP_FAILED`
- `IN_PROGRESS`
- `FINISHED`
- `CANCELED`

Current transition rules are centralized in `ActivityStatusFlow`.

Main transitions:
1. create: `DRAFT -> PENDING_REVIEW`
2. admin approve: `PENDING_REVIEW -> SIGNUP_OPEN`
3. admin reject: `PENDING_REVIEW -> DRAFT`
4. organizer cancel: `* -> CANCELED` except `FINISHED` and `CANCELED`
5. later signup/orchestration transitions are reserved in the same flow class

## Permission Design
1. Organizer-only operations:
   - update activity
   - cancel activity
2. Admin review:
   - centralized in `ActivityPermissionChecker`
   - current placeholder rule uses username `admin`
   - this is intentionally isolated so later RBAC can replace it without touching controller/service flow

## API
- `POST /api/activity/create`
- `PUT /api/activity/update/{id}`
- `GET /api/activity/{id}`
- `GET /api/activity/page`
- `POST /api/activity/cancel/{id}`
- `POST /api/admin/activity/review/{id}`

## Reserved Fields
The activity model now reserves:
- `waitlist_limit`
- `view_count`
- `heat_score`
- `reviewed_by`
- `reviewed_at`
- `review_remark`

These are included in entity/VO/DDL so later signup, waitlist, review, and recommendation logic can plug in directly.

## Notes
- edit is allowed for `DRAFT`, `PENDING_REVIEW`, or pre-signup activities with zero participants and zero waitlist users
- cancel writes status log
- review writes status log
- activity tags are replaced in batch during create/update
