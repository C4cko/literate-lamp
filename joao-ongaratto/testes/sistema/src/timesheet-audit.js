function hasSubmittedAuditEvent(timesheet, history) {
  return timesheet?.status?.id === 'SUBMITTED'
    && Array.isArray(history)
    && history.some((entry) => entry?.action?.name === 'SUBMITTED');
}

module.exports = {hasSubmittedAuditEvent};