# FileBridge

A modern, full-stack cloud file management and sharing web application featuring user authentication, Supabase private object storage, role/permission-based file sharing, freemium subscription quotas, Razorpay test mode payment integration, and real-time activity tracking.

---

## 🚀 Tech Stack

- **Frontend**: React 19, Vite 8, Tailwind CSS, React Router 7, Lucide Icons
- **Backend**: Java 21, Spring Boot 3.2.3, Spring Security 6, Spring Data MongoDB, JJWT 0.12.5, Maven
- **Database**: MongoDB 7.x
- **Cloud Storage**: Supabase Storage (Private bucket with authenticated streaming)
- **Payment Gateway**: Razorpay (Test Mode with HMAC-SHA256 signature verification)

---

## 📦 Implemented Modules & Features

### 1. Complete File & Folder Management
- Multi-file drag-and-drop and manual upload.
- Virtual folder hierarchy with breadcrumb navigation.
- In-place file renaming and moving between folders.
- Search by file name and sorting by upload date, name, and file size.
- In-browser preview for image formats (PNG, JPG, SVG, WebP) and PDF documents.

### 2. Trash & Recovery (Soft Deletion)
- Soft deletion moves files and folders to Trash without physical data loss.
- Full restore capability returning files to their original location.
- Permanent deletion cleans up physical objects from Supabase and metadata from MongoDB.
- Storage usage accurately tracks active vs trash bytes.

### 3. Supabase Cloud Storage
- Private bucket (`filebridge-files`) storing encrypted cloud objects.
- User-scoped object key paths (`users/{userId}/{uniqueFileId}`).
- Secure byte streaming through authenticated Spring Boot proxy endpoints (no public bucket exposure).
- Backward-compatible delegation preserving local files if present.

### 4. File Sharing & Access Control (Module A)
- **Share by Email**: Share files directly with any registered user by their email address.
- **Shareable Public Links**: Generate unique UUID share links for quick distribution.
- **Granular Permissions**:
  - `VIEW`: Allows recipients/link holders to preview or view the file only; download endpoints return HTTP 403 Forbidden.
  - `DOWNLOAD`: Grants both preview and file download access.
- **Instant Revocation**: Owners can revoke access or disable links at any time.
- **Shared With Me Page**: Dedicated dashboard section showing all active incoming shares.
- **Public Share View (`/share/:token`)**: Clean unauthenticated portal for link recipients to inspect and preview/download files.

### 5. Freemium Subscription & Storage Quotas (Module B)
- **Free Plan**: 100 MB cloud storage allocated automatically on sign-up.
- **Premium Plan**: 500 MB cloud storage (₹199 / month).
- **Backend Quota Enforcement**: `FileService` verifies quota before accepting uploads; rejects files exceeding limit with `StorageQuotaExceededException` (HTTP 400).
- **Storage Metrics**: Dynamic usage bar displaying percentage used, remaining bytes, and subscription plan status.

### 6. Razorpay Test Mode Payment Integration (Module C)
- Order creation via `POST /api/v1/payments/create-order`.
- Razorpay Checkout integration with test mode payment gateway.
- Cryptographic HMAC-SHA256 signature verification on `POST /api/v1/payments/verify`.
- Duplicate payment prevention rejecting replayed transaction IDs.
- Automatic server-side account upgrade to `PREMIUM`.

### 7. Recent Activity Timeline (Module D)
- Event tracking for 10 distinct actions: `FILE_UPLOADED`, `FILE_DOWNLOADED`, `FILE_RENAMED`, `FILE_MOVED`, `FILE_DELETED`, `FILE_RESTORED`, `FILE_PERMANENTLY_DELETED`, `FILE_SHARED`, `SHARE_REVOKED`, and `PLAN_UPGRADED`.
- Paginated timeline page (`/activity`) with customized status badges and relative timestamps.

---

## ⚙️ Configuration & Environment Variables

Copy `.env.example` to `.env` or set the environment variables in your system:

```env
# Supabase Cloud Storage
SUPABASE_URL=https://<your-project>.supabase.co
SUPABASE_BUCKET=filebridge-files
SUPABASE_SECRET_KEY=sb_secret_your_service_role_key

# Razorpay Test Mode
RAZORPAY_KEY_ID=rzp_test_placeholder_key
RAZORPAY_KEY_SECRET=placeholder_secret

# Quotas & Pricing
SUBSCRIPTION_FREE_LIMIT_MB=100
SUBSCRIPTION_PREMIUM_LIMIT_MB=500
SUBSCRIPTION_PREMIUM_PRICE_INR=199
```

---

## 🛠️ Running the Application

### 1. Start MongoDB
Ensure MongoDB is running locally on port `27017`:
```cmd
net start MongoDB
```

### 2. Start Backend (Spring Boot)
```cmd
cd backend
mvn spring-boot:run
```
The REST API will be available at `http://localhost:8080/api/v1`.

### 3. Start Frontend (React + Vite)
```cmd
cd frontend
npm install
npm run dev
```
Open `http://localhost:5173` in your browser.

---

## 🧪 Testing & Verification

### Run Backend Tests (100% Pass)
```cmd
cd backend
mvn test
```
Executes all 45 integration and unit tests covering authentication, ownership isolation, file operations, trash lifecycle, storage providers, file sharing, payment signatures, and activity logging.

### Build Production Frontend
```cmd
cd frontend
npm run build
```
Creates production bundle in `dist/` with 0 errors.
