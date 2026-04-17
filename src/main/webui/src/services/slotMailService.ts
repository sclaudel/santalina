import api from './api';

export interface MissingEmailInfo {
  diverId: number;
  diverName: string;
}

export interface OrganizationMailResponse {
  sent: number;
  missingEmails: MissingEmailInfo[];
}

export const slotMailService = {
  async sendOrganizationMail(
    slotId: number,
    subject: string,
    htmlBody: string,
    emailOverrides?: Record<number, string>,
    attachment?: File | null,
  ): Promise<OrganizationMailResponse> {
    const payload = { subject, htmlBody, emailOverrides: emailOverrides ?? {} };
    const fd = new FormData();
    fd.append('data', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
    if (attachment) {
      fd.append('attachment', attachment);
    }
    const res = await api.post<OrganizationMailResponse>(
      `/slots/${slotId}/mail/organization`,
      fd,
    );
    return res.data;
  },
};
