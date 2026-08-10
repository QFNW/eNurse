-- AlterTable
ALTER TABLE "alerts" ADD COLUMN     "relatedId" TEXT;

-- CreateIndex
CREATE INDEX "alerts_userId_type_relatedId_isResolved_idx" ON "alerts"("userId", "type", "relatedId", "isResolved");
