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
  ): Promise<OrganizationMailResponse> {
    const res = await api.post<OrganizationMailResponse>(
      `/slots/${slotId}/mail/organization`,
      { subject, htmlBody, emailOverrides: emailOverrides ?? {} },
    );
    return res.data;
  },
};
